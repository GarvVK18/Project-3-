package com.iam.server.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConsentController {

    private final RegisteredClientRepository registeredClientRepository;

    public ConsentController(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @GetMapping("/oauth2/consent")
    public String consent(
            Principal principal,
            Model model,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "scope") String scope,
            @RequestParam(name = "state") String state) {

        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
        String clientName = (registeredClient != null && registeredClient.getClientName() != null)
                ? registeredClient.getClientName()
                : clientId;

        Set<String> scopesToApprove = new HashSet<>();
        if (scope != null && !scope.isBlank()) {
            Collections.addAll(scopesToApprove, scope.split(" "));
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", clientName);
        model.addAttribute("state", state);
        model.addAttribute("scopes", scopesToApprove);
        model.addAttribute("principalName", principal != null ? principal.getName() : "Authenticated User");

        return "consent";
    }
}
