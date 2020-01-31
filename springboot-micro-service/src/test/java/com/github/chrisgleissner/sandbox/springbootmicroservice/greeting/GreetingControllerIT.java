package com.github.chrisgleissner.sandbox.springbootmicroservice.greeting;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GreetingControllerIT {

    @Autowired
    private TestRestTemplate template;

    @WithMockUser(value = "admin")
    @Test
    public void greetingViaRestTemplate() {
        ResponseEntity<Greeting> result = template.withBasicAuth("spring", "secret")
                .getForEntity("/greeting?name=John", Greeting.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody().getContent()).isEqualTo("Hello John!");
    }

    @WithMockUser(value = "admin")
    @Test
    public void greetingViaRestAssured() {
        ResponseEntity<Greeting> result = template.withBasicAuth("spring", "secret")
                .getForEntity("/greeting?name=John", Greeting.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertThat(result.getBody().getContent()).isEqualTo("Hello John!");
    }
}
