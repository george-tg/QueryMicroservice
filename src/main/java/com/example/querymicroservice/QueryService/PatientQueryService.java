package com.example.querymicroservice.QueryService;


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

    static PatientDTO convertToDTO(Patient patient) {
        // Convert Patient entity to DTO
        // Implement this method based on your DTO structure
        return new PatientDTO(patient.getId(), patient.getFirstName(), patient.getLastName(), patient.getAge());
    }

    public PatientDetailsDTO readQuery(Long patientId) {
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

            PatientDetailsDTO patientDTO = new PatientDetailsDTO(patient.getId(), conditionDTOS,observationDTOS,encounterDTOS, patient.getFirstName(), patient.getLastName(), patient.getAge());
            logger.info("Getting patient: " + patientDTO.getFirstName() + patientDTO.getLastName());
            return patientDTO;
        } else {
            // Handle not found scenario (log or send error response)
            System.out.println("Patient not found for ID: " + patientId);
        }
        return null;
    }

    public List<PatientDTO> readAllPatientsQuery() {
        List<Patient> patients = patientRepository.findAll();
        List<PatientDTO> patientDTOS = new ArrayList<>();
        if (patients != null) {
            for (Patient patient: patients) {
                patientDTOS.add(convertToDTO(patient));
            }
            logger.info("Getting patients: " + patients);
            return patientDTOS;
        } else {
            // Handle not found scenario (log or send error response)
            System.out.println("Patients not found");
        }
        return null;
    }


    @KafkaListener(topics = "delete_patient_event", groupId = "patient_group")
    public void consumeDeletePatientEvent(Long id) {
        patientRepository.deleteById(id);
    }
    @KafkaListener(topics = "create_patient_event", groupId = "patient_group")
    public void consumeCreatePatientEvent(String jsonPayload) throws JsonProcessingException {
        PatientDTO patient = objectMapper.readValue(jsonPayload, PatientDTO.class);
        Patient createdPatient = new Patient(patient.getFirstName(), patient.getLastName(), patient.getAge(), patient.getUserId());
        patientRepository.save(createdPatient);
    }
    @KafkaListener(topics = "update_patient_event", groupId = "patient_group")
    public void consumeUpdatePatient(ConsumerRecord<String, PatientDTO> record) {
        String idString = record.key(); // Extract the patient ID string from the message key
        Long id = Long.parseLong(idString); // Convert the patient ID string to Long
        PatientDTO patientDTO = record.value(); // Extract the patient DTO from the message value

        Patient p = new Patient(patientDTO.getFirstName(), patientDTO.getLastName(), patientDTO.getAge());
        Optional<Patient> existing = patientRepository.findById(id);
        if (existing.isPresent()) {
            p.setId(id);
            patientRepository.save(p);
        } else {
            throw new RuntimeException("Can't update patient " + id);
        }
    }


}
