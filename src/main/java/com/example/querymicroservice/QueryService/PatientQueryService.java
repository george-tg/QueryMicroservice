package com.example.querymicroservice.QueryService;


import com.example.querymicroservice.Config.AccessTokenUser;
import com.example.querymicroservice.KeycloakTokenExchangeService;
import com.example.querymicroservice.QueryRepository.PatientRepository;
import com.example.querymicroservice.domain.Condition;
import com.example.querymicroservice.domain.Encounter;
import com.example.querymicroservice.domain.Observation;
import com.example.querymicroservice.domain.Patient;
import com.example.querymicroservice.dtos.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PatientQueryService
{    private static final Logger logger = LoggerFactory.getLogger(PatientQueryService.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeycloakTokenExchangeService keycloakTokenExchangeService;


    static PatientDTO convertToDTO(Patient patient) {
        // Convert Patient entity to DTO
        // Implement this method based on your DTO structure
        return new PatientDTO(patient.getId(), patient.getFirstName(), patient.getLastName(), patient.getAge());
    }

    public PatientDetailsDTO readQuery(Long patientId) {
        AccessTokenUser accessTokenUser = AccessTokenUser.convert(SecurityContextHolder.getContext());
        String reducedScopes = "patient";
        accessTokenUser = keycloakTokenExchangeService.getLimitedScopeToken(accessTokenUser, reducedScopes);
        List<String> scopes = accessTokenUser.getScopes();
        if(scopes.size() == 1 && scopes.get(0).equals("patient")) {
            Patient patient = patientRepository.findById(patientId).orElse(null);
            List<Encounter> encounters = patient.getEncounters();
            List<Observation> observations = patient.getObservations();
            List<Condition> conditions = patient.getConditions();
            if (patient != null) {
                List<ConditionDTO> conditionDTOS = new ArrayList<>();
                List<ObservationDTO> observationDTOS = new ArrayList<>();
                List<EncounterDTO> encounterDTOS = new ArrayList<>();

                for (Condition condition : conditions) {
                    conditionDTOS.add(ConditionQueryService.convertConditionToDTO(condition));
                }
                for (Encounter encounter : encounters) {
                    encounterDTOS.add(EncounterQueryService.convertToDTO(encounter));
                }
                for (Observation observation : observations) {
                    observationDTOS.add(ObservationQueryService.convertToDTO(observation));
                }

                PatientDetailsDTO patientDTO = new PatientDetailsDTO(patient.getId(), conditionDTOS, observationDTOS, encounterDTOS, patient.getFirstName(), patient.getLastName(), patient.getAge());
                logger.info("Getting patient: " + patientDTO.getFirstName() + patientDTO.getLastName());
                return patientDTO;
            } else {
                // Handle not found scenario (log or send error response)
                System.out.println("Patient not found for ID: " + patientId);
            }
        }
        return null;
    }

    public List<PatientDTO> readAllPatientsQuery() {
        AccessTokenUser accessTokenUser = AccessTokenUser.convert(SecurityContextHolder.getContext());
        String reducedScopes = "patient";
        accessTokenUser = keycloakTokenExchangeService.getLimitedScopeToken(accessTokenUser, reducedScopes);
        List<String> scopes = accessTokenUser.getScopes();
        if(scopes.size() == 1 && scopes.get(0).equals("patient")) {
            List<Patient> patients = patientRepository.findAll();
            List<PatientDTO> patientDTOS = new ArrayList<>();
            if (patients != null) {
                for (Patient patient : patients) {
                    patientDTOS.add(convertToDTO(patient));
                }
                logger.info("Getting patients: " + patients);
                return patientDTOS;
            } else {
                // Handle not found scenario (log or send error response)
                System.out.println("Patients not found");
            }
        }
        return null;
    }


    @KafkaListener(topics = "delete_patient_event", groupId = "patient_group")
    public void consumeDeletePatientEvent(String jsonPayload) {
        try {
            PatientDTO patient = objectMapper.readValue(jsonPayload, PatientDTO.class);
            List<String> scopes = patient.getAccessTokenUser().getScopes();
            if(scopes.size() == 1 && scopes.get(0).equals("patient")) {
                patientRepository.deleteById(patient.getId());
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing PatientDTO to JSON while deleting patient: " + e.getMessage(), e);
        }
    }
    @KafkaListener(topics = "create_patient_event", groupId = "patient_group")
    public void consumeCreatePatientEvent(String jsonPayload) {
        try {
            PatientDTO patient = objectMapper.readValue(jsonPayload, PatientDTO.class);
            List<String> scopes = patient.getAccessTokenUser().getScopes();
            if(scopes.size() == 1 && scopes.get(0).equals("patient")) {
                Patient createdPatient = new Patient(patient.getFirstName(), patient.getLastName(), patient.getAge(), patient.getUserId());
                patientRepository.save(createdPatient);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing PatientDTO to JSON while creating patient: " + e.getMessage(), e);
        }
    }
    @KafkaListener(topics = "update_patient_event", groupId = "patient_group")
    public void consumeUpdatePatient(String jsonPayload) {
        try {
            PatientDTO patientDTO = objectMapper.readValue(jsonPayload, PatientDTO.class);
            List<String> scopes = patientDTO.getAccessTokenUser().getScopes();
            if(scopes.size() == 1 && scopes.get(0).equals("patient")) {
                Long id = patientDTO.getId();
                Patient p = new Patient(patientDTO.getFirstName(), patientDTO.getLastName(), patientDTO.getAge());
                Optional<Patient> existing = patientRepository.findById(id);
                if (existing.isPresent()) {
                    p.setId(id);
                    patientRepository.save(p);
                } else {
                    throw new RuntimeException("Can't update patient " + id);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing PatientDTO to JSON while updating patient: " + e.getMessage(), e);
        }
    }


}
