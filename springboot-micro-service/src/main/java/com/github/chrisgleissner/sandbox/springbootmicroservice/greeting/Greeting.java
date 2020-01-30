package com.github.chrisgleissner.sandbox.springbootmicroservice.greeting;

import lombok.Value;

@Value
public class Greeting {
    private final long id;
    private final String content;
}
