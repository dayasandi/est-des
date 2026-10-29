# est-des

Backend de gestão de estacionamento: recebe os eventos de entrada, estacionamento e saída de veículos, calcula o valor a cobrar com preço dinâmico por lotação e informa o faturamento por dia e por setor.

## Sumário

1. [Visão geral](#visão-geral)
2. [Como subir o projeto](#como-subir-o-projeto)
3. [Como testar](#como-testar)
4. [API](#api)
5. [Regras de negócio](#regras-de-negócio)
6. [Arquitetura e módulos](#arquitetura-e-módulos)
7. [Decisões técnicas e por quê](#decisões-técnicas-e-por-quê)
8. [Observabilidade](#observabilidade)
9. [Rate limit](#rate-limit)
10. [Configuração](#configuração)

## Visão geral

Um simulador de garagem expõe `GET /garage` na porta 3000 com a configuração (setores, preço base, capacidade e as vagas com suas coordenadas). Ao receber a primeira chamada a esse endpoint, ele passa a enviar eventos para o webhook da aplicação em `http://localhost:3003/webhook`.

A aplicação:

- carrega a configuração da garagem no startup e só passa a aceitar eventos depois disso;
- processa os eventos `ENTRY`, `PARKED` e `EXIT`;
- cobra na saída, aplicando o fator de preço definido no momento em que o veículo estacionou;
- responde `GET /revenue` com o faturamento realizado de um dia, total ou por setor.

Stack: Java 21, Spring Boot 4.1, Gradle (Kotlin DSL), MySQL 8.4, Flyway, Log4j2, Micrometer/Prometheus, Bucket4j.

## Como subir o projeto

### Opção 1: tudo com Docker Compose (recomendada)

Pré-requisito: Docker com Compose.

```bash
docker compose up -d --build
```

Isso sobe três containers:

| Serviço | Papel | Porta |
|---|---|---|
| `mysql` | Banco de dados (MySQL 8.4, sem porta publicada no host) | 3306 interna |
| `simulator` | Simulador da garagem (`cfontes0estapar/garage-sim:1.0.0`) | 3000 e 3003 em `127.0.0.1` |
| `app` | Esta aplicação | 3003 (rede do simulador) |

Acompanhar:

```bash
docker compose ps
docker compose logs -f app
```

Verificar se está pronto:

```bash
curl http://127.0.0.1:3003/actuator/health/readiness
```

Consultar o faturamento do dia (UTC):

```bash
curl "http://127.0.0.1:3003/revenue?date=$(date -u +%Y-%m-%d)"
```

Derrubar (o `-v` apaga o volume do banco):

```bash
docker compose down -v
```

O compose roda com logs em JSON (`SPRING_PROFILES_ACTIVE=json`). Para logs em texto:

```bash
SPRING_PROFILES_ACTIVE=default docker compose up -d --build
```

### Opção 2: aplicação local com Gradle

Pré-requisitos: JDK 21 (há um `.sdkmanrc` com a versão) e um MySQL 8.x acessível.

Banco de desenvolvimento rápido com Docker:

```bash
docker run -d --name garage-mysql -p 3306:3306 -e MYSQL_DATABASE=garage -e MYSQL_USER=garage -e MYSQL_PASSWORD=garage -e MYSQL_RANDOM_ROOT_PASSWORD=yes mysql:8.4
```

Aplicação:

```bash
./gradlew :app:bootRun
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

Sem o simulador respondendo em `http://localhost:3000`, a aplicação tenta sincronizar a garagem com backoff exponencial, fica com o readiness em `DOWN` e responde `503` nos endpoints de negócio até conseguir. Os eventos do simulador só chegam ao webhook quando ele roda na mesma rede da aplicação (como no compose), porque ele sempre chama `localhost:3003` de dentro do próprio container.

### Gerar o jar

```bash
./gradlew :app:bootJar
```

O artefato fica em `app/build/libs/app.jar`.

## Como testar

Pré-requisito: Docker em execução, porque os testes de integração usam MySQL real via Testcontainers.

```bash
./gradlew check
```

Esse comando executa:

- `:core:test`: testes unitários do domínio e dos casos de uso, sem Spring e sem banco;
- `:app:test`: testes de adaptadores, contratos de repositório contra MySQL real, testes ponta a ponta (regras de negócio, concorrência, rate limit, observabilidade, garagem não carregada) e as regras de arquitetura (ArchUnit);
- `:app:jsonLoggingTest`: valida o formato JSON dos logs em uma JVM separada, porque o Log4j2 é inicializado uma única vez por JVM.

Só o núcleo, sem Docker:

```bash
./gradlew :core:test
```

## API

Porta `3003`.

### `POST /webhook`

Recebe os eventos do simulador.

```json
{"license_plate":"ZUL0001","entry_time":"2026-10-29T10:00:00.000Z","event_type":"ENTRY"}
{"license_plate":"ZUL0001","lat":-23.561684,"lng":-46.655981,"event_type":"PARKED"}
{"license_plate":"ZUL0001","exit_time":"2026-10-29T12:10:00.000Z","event_type":"EXIT"}
```

| Situação | Status |
|---|---|
| Evento aceito | 200 |
| `EXIT` repetido ou de placa sem permanência ativa (ignorado, sem nova cobrança) | 200 |
| Corpo inválido, campo obrigatório ausente, `event_type` desconhecido | 400 |
| `ENTRY` com a garagem lotada | 409 |
| `ENTRY` de placa que já está dentro | 409 |
| `PARKED` sem `ENTRY` ativo, vaga já ocupada, ou veículo já estacionado em outra vaga | 409 |
| `PARKED` em coordenada que não corresponde a nenhuma vaga | 422 |
| Método ou tipo de conteúdo não suportado | 405 e 415 |
| Rate limit excedido | 429 |
| Garagem ainda não carregada ou banco indisponível (com `Retry-After`) | 503 |
| Erro inesperado (sem expor a causa) | 500 |

Os erros seguem o formato `application/problem+json` (RFC 9457).

### `GET /revenue`

Parâmetros por query string (`date` no formato `YYYY-MM-DD` e `sector` opcional). Também aceita o mesmo conteúdo em JSON no corpo da requisição. Sem `date`, usa o dia atual em UTC.

```bash
curl "http://127.0.0.1:3003/revenue?date=2026-10-29&sector=A"
```

```json
{"amount":133.65,"currency":"BRL","timestamp":"2026-10-29T03:36:44.698Z"}
```

| Situação | Status |
|---|---|
| Sucesso | 200 |
| Data ou setor com formato inválido | 400 |
| Setor inexistente | 404 |

## Regras de negócio

**Capacidade.** A garagem tem capacidade total igual à soma da capacidade dos setores. Cada `ENTRY` reserva uma unidade; se não houver, a entrada é recusada com 409. A reserva só é liberada quando a saída é de fato efetivada.

**Setor da permanência.** O setor é definido no `PARKED`, a partir das coordenadas da vaga.

**Preço dinâmico.** No momento do `PARKED`, calcula-se a lotação do setor (veículos já estacionados, sem contar o próprio veículo) sobre a capacidade do setor e escolhe-se o fator:

| Lotação | Fator |
|---|---|
| 0% a menos de 25% | 0,90 |
| 25% a menos de 50% | 1,00 |
| 50% a menos de 75% | 1,10 |
| 75% a 100% | 1,25 |

O fator e o preço base ficam gravados na permanência e não mudam depois.

**Cobrança.** Até 30 minutos entre `entry_time` e `exit_time`, não há cobrança. Acima disso, cobra-se por hora iniciada: `horas arredondadas para cima × preço base × fator`, com arredondamento para cima na casa dos centavos. 30 minutos exatos são gratuitos; 30 minutos e 1 segundo cobram 1 hora.

**Faturamento.** Soma apenas saídas efetivadas, agrupadas pelo dia UTC do `exit_time`. Veículos que ainda estão dentro da garagem não entram na soma.

**Datas.** Timestamps sem fuso são interpretados como UTC. Tudo é armazenado em UTC com precisão de milissegundos.

**Idempotência e anomalias.** Um `EXIT` repetido ou sem permanência ativa não cobra de novo e não libera vaga duas vezes. Isso é garantido também sob concorrência.

## Arquitetura e módulos

O projeto segue arquitetura hexagonal (portas e adaptadores) e é dividido em dois módulos Gradle.

```
est-des
├── core   Java puro. Domínio e casos de uso. Sem Spring, sem JDBC, sem HTTP.
└── app    Adaptadores e composição: Spring Boot, web, persistência, simulador, observabilidade.
```

### Módulo `core`

Não depende de nenhum framework. Só enxerga a si mesmo.

```
core/src/main/java/br/com/garage
├── domain
│   ├── garage     Sector, Spot, Coordinates, GarageConfig, Occupancy, SectorCode
│   ├── parking    ParkingSession (ENTERED, PARKED, CLOSED), LicensePlate
│   ├── pricing    PricingPolicy, PriceTable, PriceBand, ParkingFee
│   └── shared     Money
└── application
    ├── port
    │   ├── inbound    Casos de uso expostos: RegisterEntry, RegisterParking, RegisterExit, GetRevenue, SyncGarage
    │   └── outbound   O que o núcleo precisa do mundo: SessionRepository, SpotRepository, SectorRepository,
    │                  CapacityGate, RevenueReader, GarageConfigProvider, TransactionRunner
    └── usecase        Implementações dos casos de uso
```

O módulo também publica `testFixtures` com implementações em memória das portas e testes de contrato abstratos. O mesmo contrato roda contra a implementação em memória e contra a implementação JDBC, o que garante que o fake usado nos testes unitários se comporta como o banco real.

### Módulo `app`

```
app/src/main/java/br/com/garage
├── inbound
│   ├── web          Controllers, tratamento de erros, filtro de log por requisição, guarda de readiness
│   ├── sync         Carga da garagem no startup com retry e backoff, estado de prontidão
│   └── ratelimit    Filtro de rate limit (Bucket4j + Caffeine)
├── outbound
│   ├── persistence  Repositórios JDBC, controle transacional com retry em conflito
│   └── simulator    Cliente HTTP do GET /garage
├── observability    Métricas de negócio, indicador de saúde da sincronização, decoradores dos casos de uso
└── config           Montagem dos beans (a única camada que conhece core e adaptadores juntos)
```

### Regras de dependência (verificadas por teste)

A classe `ArchitectureTest` (ArchUnit) quebra o build se alguém violar:

- o domínio não conhece frameworks nem as camadas externas;
- a camada de aplicação não conhece frameworks nem adaptadores;
- os casos de uso só dependem de portas e do domínio;
- adaptadores de entrada e de saída não se conhecem entre si;
- nada depende da camada de configuração, que só monta os beans;
- só a persistência (e a configuração que a monta) fala SQL.

### Fluxo de um evento

```
POST /webhook -> RateLimitFilter -> RequestLoggingFilter -> GarageReadyInterceptor
              -> WebhookController -> WebhookDispatcher -> caso de uso (via decorador de métricas)
              -> portas de saída -> adaptadores JDBC -> MySQL
```

## Decisões técnicas e por quê

**Hexagonal com dois módulos, e não só pacotes.** Separar `core` em outro módulo faz o compilador impedir que o domínio importe Spring. A regra deixa de depender de disciplina e passa a ser mecânica. O custo é um módulo a mais, que é pequeno diante do ganho de testes de domínio e de casos de uso rápidos e sem contexto Spring.

**Casos de uso como interfaces de porta de entrada.** O controller depende de `RegisterExit`, não da classe concreta. Isso permite decorar o caso de uso com métricas (`MeteredRegisterExit`) sem tocar na regra de negócio nem no controller.

**MySQL com invariantes no próprio banco.** As regras que não podem ser violadas sob concorrência estão no schema, não só no código:
- uma permanência ativa por placa: coluna gerada `active_plate` com índice `UNIQUE`;
- um único veículo por vaga: coluna gerada `occupied_spot` com índice `UNIQUE`;
- capacidade: uma linha contadora atualizada com `UPDATE ... WHERE reserved < total`, que é atômico e não precisa de lock explícito.

Assim, mesmo que duas requisições passem pela validação em memória ao mesmo tempo, apenas uma consegue gravar.

**`close()` devolve boolean.** Dois `EXIT` simultâneos da mesma placa chegavam a cobrar e liberar capacidade duas vezes. Agora o encerramento da permanência informa se de fato fechou algo, e o caso de uso só cobra e libera a vaga quando fechou. Há teste de concorrência cobrindo isso.

**Retry de transação em conflito.** Deadlock e falha de serialização do InnoDB são esperados sob concorrência. A transação é repetida até 3 vezes em `ConcurrencyFailureException`. Se o banco estiver indisponível, a resposta é 503 com `Retry-After`.

**`JdbcClient` em vez de JPA.** O domínio é pequeno, o SQL é parte da solução (colunas geradas, update condicional) e o mapeamento objeto-relacional adicionaria uma segunda representação das entidades. SQL explícito é mais fácil de revisar e de testar contra o banco real.

**Flyway.** O schema é versionado e aplicado no startup. Os testes e o ambiente real usam o mesmo script.

**Preço fixado no `PARKED`.** O enunciado liga o fator à lotação no momento em que o veículo ocupa a vaga. Recalcular na saída faria o valor depender de quem entrou ou saiu depois, o que não é justo nem reproduzível. Por isso o fator e o preço base são gravados na permanência.

**Valores monetários em `BigDecimal` com arredondamento para cima.** Nada de `double` em dinheiro. O arredondamento para cima nos centavos é explícito e coberto por teste.

**Faturamento só de saídas efetivadas, por dia UTC da saída.** Receita é o que foi cobrado. O dia UTC evita ambiguidade de fuso e bate com a forma como os timestamps chegam.

**Sincronização com retry e prontidão.** Se o simulador ainda não estiver no ar quando a aplicação sobe, ela não cai: tenta de novo com backoff exponencial, mantém o readiness em `DOWN` e responde 503 com `Retry-After` até a garagem carregar. Em orquestradores isso evita receber tráfego antes da hora.

**Sem circuit breaker.** A única dependência síncrona do caminho de negócio é o MySQL, e o simulador só é chamado na carga inicial (já com retry). Um circuit breaker sobre o banco não traria ganho real: sem banco não há resposta útil possível. A proteção adotada é o 503 explícito com `Retry-After` e o rate limit.

**Compose com a aplicação na rede do simulador.** O simulador sempre envia os eventos para `localhost:3003`. Para isso funcionar sem alterar a imagem dele, a aplicação compartilha o namespace de rede do container do simulador (`network_mode: service:simulator`). O banco fica numa rede interna e não é publicado no host.

**Virtual threads.** O processamento é bloqueante (JDBC). Threads virtuais permitem muita concorrência sem dimensionar pool de threads de servlet.

**Log4j2 com perfil JSON.** Logs em texto com `request_id` por padrão e JSON estruturado (formato logstash) com o perfil `json`, que é o usado no compose. Níveis explícitos por pacote em `application.yml`.

**Container endurecido.** Imagem multi-stage, usuário não root, sistema de arquivos somente leitura, `cap_drop: ALL`, `no-new-privileges` e limites de memória.

**Compilação estrita.** `-Xlint:all -Werror`: avisos, inclusive de APIs obsoletas, quebram o build.

## Observabilidade

Endpoints do Actuator em `http://127.0.0.1:3003/actuator`:

| Endpoint | Uso |
|---|---|
| `/health/liveness` | O processo está vivo |
| `/health/readiness` | Pronto para tráfego: estado da aplicação, banco e garagem sincronizada |
| `/prometheus` | Métricas para scraping |
| `/metrics`, `/info` | Inspeção |

Métricas de negócio:

- `garage_events_total{event,outcome}`: eventos processados por tipo e resultado (`accepted`, `charged`, `no_active_session`, `unknown_spot`, `no_active_entry`, entre outros);
- `garage_exit_charged`: distribuição dos valores cobrados em BRL;
- `garage_capacity{state=total|reserved}`: capacidade total e reservada;
- `garage_rate_limit_rejected_total{policy}`: requisições barradas pelo rate limit.

Além disso, as métricas HTTP padrão (`http_server_requests_seconds`) com histograma, e as de JVM e do pool de conexões.

Logs: cada requisição recebe um `request_id` (MDC) que aparece em todas as linhas relacionadas. Os eventos de negócio registram campos estruturados como `event`, `plate`, `sector` e `price_factor`.

## Rate limit

Token bucket por cliente (Bucket4j com cache Caffeine), com política separada por rota:

| Rota | Rajada | Reposição |
|---|---|---|
| `/webhook` | 200 | 100 por segundo |
| `/revenue` | 30 | 10 por segundo |

Acima do limite a resposta é `429`, com `Retry-After`, e a métrica `garage_rate_limit_rejected_total` é incrementada. O webhook tem limite alto de propósito, para não barrar o fluxo legítimo do simulador. Os endpoints do Actuator não passam pelo rate limit.

## Configuração

Todas as propriedades têm valor padrão e podem ser sobrescritas por variável de ambiente.

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/garage` | URL JDBC |
| `DB_USER` | `garage` | Usuário do banco |
| `DB_PASSWORD` | `garage` | Senha do banco |
| `SIMULATOR_URL` | `http://localhost:3000` | Base do simulador |
| `SPRING_PROFILES_ACTIVE` | vazio (`json` no compose) | `json` ativa logs estruturados |
| `LOG_LEVEL_ROOT` | `INFO` | Nível raiz dos logs |
| `LOG_LEVEL_APP` | `INFO` | Nível dos logs de `br.com.garage` |

As credenciais padrão são apenas para desenvolvimento. Em outros ambientes, injete-as por variável de ambiente ou pelo mecanismo de segredos da plataforma.
