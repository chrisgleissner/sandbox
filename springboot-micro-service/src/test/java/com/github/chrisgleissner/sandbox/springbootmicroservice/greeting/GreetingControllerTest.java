package com.github.chrisgleissner.sandbox.springbootmicroservice.greeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(GreetingController.class)
public class GreetingControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @WithMockUser(value = "admin")
    @Test
    public void greetingViaMockMvc() throws Exception {
        String responseBody = mvc.perform(get("/greeting?name=John")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Greeting greeting = objectMapper.readValue(responseBody, Greeting.class);
        assertThat(greeting.getContent()).isEqualTo("Hello John!");
    }

    @Ignore
    @Test
    public void greetingViaRestAssured() {
        given()
                .standaloneSetup(new GreetingController())
                .when()
                .get("/greeting?name=John")
                .then()
                .log().ifValidationFails()
                .statusCode(OK.value())
                .contentType(JSON)
                .body(is(equalTo("Hello John!")));
    }
}
