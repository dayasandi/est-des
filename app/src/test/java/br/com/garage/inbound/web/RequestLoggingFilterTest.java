package br.com.garage.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    private String callAndCaptureMdc(String incomingId, MockHttpServletResponse response) throws Exception {
        var request = new MockHttpServletRequest("POST", "/webhook");
        if (incomingId != null) {
            request.addHeader("X-Request-Id", incomingId);
        }
        List<String> seen = new ArrayList<>();
        filter.doFilter(request, response, (req, res) -> seen.add(MDC.get("request_id")));
        return seen.getFirst();
    }

    @Test
    void geraUmIdQuandoNaoVemNaRequisicaoEExpoeNoMdcENaResposta() throws Exception {
        var response = new MockHttpServletResponse();

        String idInMdc = callAndCaptureMdc(null, response);

        assertThat(idInMdc).isNotBlank();
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(idInMdc);
    }

    @Test
    void reaproveitaOIdRecebidoQuandoEleESeguro() throws Exception {
        var response = new MockHttpServletResponse();

        String idInMdc = callAndCaptureMdc("abc-123_XYZ.9", response);

        assertThat(idInMdc).isEqualTo("abc-123_XYZ.9");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc-123_XYZ.9");
    }

    @Test
    void descartaIdForjadoComQuebraDeLinhaOuExcessoDeTamanho() throws Exception {
        String forged = "ok\nINFO falso: usuario admin logou";

        String idInMdc = callAndCaptureMdc(forged, new MockHttpServletResponse());
        String tooLong = callAndCaptureMdc("a".repeat(65), new MockHttpServletResponse());

        assertThat(idInMdc).isNotEqualTo(forged).matches("[0-9a-f-]{36}");
        assertThat(tooLong).matches("[0-9a-f-]{36}");
    }

    @Test
    void limpaOMdcAoFinalMesmoQuandoACadeiaFalha() throws Exception {
        var request = new MockHttpServletRequest("POST", "/webhook");

        try {
            filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
                throw new IllegalStateException("falha");
            });
        } catch (IllegalStateException expected) {
        }

        assertThat(MDC.get("request_id")).isNull();
    }
}
