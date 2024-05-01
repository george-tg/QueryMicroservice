package com.example.querymicroservice.QueryController;

import com.example.querymicroservice.Config.AccessTokenUser;
import com.example.querymicroservice.KeycloakTokenExchangeService;
import com.example.querymicroservice.QueryService.PatientQueryService;
import com.example.querymicroservice.dtos.PatientDTO;
import com.example.querymicroservice.dtos.PatientDetailsDTO;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
public class PatientQueryController
{
    private static final Logger logger = LoggerFactory.getLogger(PatientQueryService.class);
    @Autowired
    private KeycloakTokenExchangeService keycloakTokenExchangeService;
    @Autowired
    private PatientQueryService patientEventConsumer;
    @GetMapping("/retrieve/{patientId}")
    @PreAuthorize("hasRole('ROLE_doctor')")
    public ResponseEntity<PatientDetailsDTO> getPatientDetails(@PathVariable Long patientId) throws TimeoutException {

        logger.warn("Token1: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        PatientDetailsDTO patientDTO = patientEventConsumer.readQuery(patientId);
        return ResponseEntity.ok(patientDTO);
    }
    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_doctor')")
    public ResponseEntity<List<PatientDTO>> getAllPatient() {
        try {
            AccessTokenUser accessTokenUser = AccessTokenUser.convert(SecurityContextHolder.getContext());
            logger.warn("Token: " + accessTokenUser);
            Map response = keycloakTokenExchangeService.getLimitedScopeToken(accessTokenUser.getToken());
            accessTokenUser.setScopes(Arrays.stream(response.get("scope").toString().split(" ")).toList());
            accessTokenUser.setToken(response.get("access_token").toString());
            logger.warn("Tokenexchagne : " + accessTokenUser);

            return ResponseEntity.ok(patientEventConsumer.readAllPatientsQuery());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
