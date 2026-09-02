package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserControllerTest {

    private final UserController userController = new UserController();

    @Test
    void getUser_shouldReturnSuccessMessage() {
        String response = userController.getUser();

        assertEquals(
                "User API is working. JWT token is valid.",
                response
        );
    }
}