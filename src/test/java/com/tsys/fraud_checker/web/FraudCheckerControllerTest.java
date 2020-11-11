package com.tsys.fraud_checker.web;

import com.tsys.fraud_checker.domain.CreditCard;
import com.tsys.fraud_checker.domain.Money;
import com.tsys.fraud_checker.services.VerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

// For Junit4, use @RunWith
// @RunWith(SpringRunner.class)
// For Junit5, use @ExtendWith
@ExtendWith(SpringExtension.class)
// We're only testing the web layer, we use the @WebMvcTest
// annotation. It allows us to easily test requests and responses
// using the set of static methods implemented by the
// MockMvcRequestBuilders and MockMvcResultMatchers classes.
@WebMvcTest(FraudCheckerController.class)
@AutoConfigureMockMvc
// We verify the validation behavior with an integration test:
public class FraudCheckerControllerTest {

  @MockBean
  private VerificationService verificationService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void health() throws Exception {
    final var request = givenRequestFor("/ping", false);
    final ResultActions resultActions = whenTheRequestIsMade(request);
    thenExpect(resultActions,
            MockMvcResultMatchers.status().isOk(),
            MockMvcResultMatchers.content().bytes("{ 'PONG' : 'FraudCheckerController is running fine!' }".getBytes()));
  }

  @Test
  public void homesToIndexPage() throws Exception {
    final var request = givenRequestFor("/", false);
    final ResultActions resultActions = whenTheRequestIsMade(request);
    thenExpect(resultActions,
            MockMvcResultMatchers.status().isOk());
  }

  @Test
  public void shoutsWhenThereIsAProblemWithCheckingCardFraud() throws Exception {
    given(verificationService.verifyTransactionAuthenticity(any(CreditCard.class), any(Money.class)))
            .willThrow(new InterruptedException());

    final var request = givenRequestFor("/check", true)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\n" +
                    "    \"creditCard\" : {\n" +
                    "        \"number\": \"4485-2847-2013-4093\",\n" +
                    "        \"holderName\" : \"Jumping Jack\",\n" +
                    "        \"issuingBank\" : \"Bank of America\",\n" +
                    "        \"validUntil\" : \"2020-10-04T01:00:26.874+00:00\",\n" +
                    "        \"cvv\" : 123\n" +
                    "    },\n" +
                    "    \"charge\" : {\n" +
                    "        \"currency\" : \"INR\",\n" +
                    "        \"amount\" : 1235.45\n" +
                    "    }\n" +
                    "}");

    final ResultActions resultActions = whenTheRequestIsMade(request);

    thenExpect(resultActions,
            MockMvcResultMatchers.status().isInternalServerError());
  }

  private MockHttpServletRequestBuilder givenRequestFor(String url, boolean isPostRequest) {
    if (isPostRequest)
      return MockMvcRequestBuilders.post(url).characterEncoding("UTF-8");

    return MockMvcRequestBuilders.get(url).characterEncoding("UTF-8");
  }

  private ResultActions whenTheRequestIsMade(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request);
  }

  private void thenExpect(ResultActions resultActions, ResultMatcher... matchers) throws Exception {
    resultActions.andExpect(ResultMatcher.matchAll(matchers));
  }
}