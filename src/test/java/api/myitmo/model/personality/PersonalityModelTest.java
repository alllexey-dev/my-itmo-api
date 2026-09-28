package api.myitmo.model.personality;

import api.myitmo.MyItmo;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalityModelTest {
    private final Gson gson = new MyItmo().getGson();

    @Test
    void studentProfileKeepsStringCourseAndEmptyEmployeeLists() {
        String json = "{\"isu\":123456,\"fio\":\"Тестовый Студент\",\"gender\":\"male\","
                + "\"photo\":\"https://example.test/student.jpg\",\"contacts\":[],\"rooms\":[],\"positions\":[],"
                + "\"powers\":[],\"levels\":null,\"education\":[{\"course\":\"3\",\"group\":\"T1234\","
                + "\"faculty_name\":\"Тестовый факультет\"}],\"activities\":null,\"exchange_training\":false}";

        Personality personality = gson.fromJson(json, Personality.class);

        assertEquals(123456L, personality.getIsu());
        assertEquals("Тестовый Студент", personality.getFio());
        assertEquals("male", personality.getGender());
        assertEquals("https://example.test/student.jpg", personality.getPhotoUrl());
        assertTrue(personality.getContacts().isEmpty());
        assertTrue(personality.getRooms().isEmpty());
        assertTrue(personality.getPositions().isEmpty());
        assertEquals(1, personality.getEducation().size());
        Education education = personality.getEducation().get(0);
        assertEquals("3", education.getCourse());
        assertEquals("T1234", education.getGroup());
        assertEquals("Тестовый факультет", education.getFacultyName());
        assertFalse(personality.isExchangeTraining());
    }

    @Test
    void employeeProfileKeepsContactsAndPositionsAndIgnoresUnmodelledFields() {
        String json = "{\"isu\":234567,\"fio\":\"Тестовый Сотрудник\",\"gender\":\"male\","
                + "\"photo\":\"https://example.test/employee.jpg\",\"contacts\":[{"
                + "\"contact\":[\"teacher@example.test\"],\"contact_alias\":\"Электронная почта\"}],"
                + "\"rooms\":[],\"positions\":[{\"department_name\":\"Тестовое подразделение\","
                + "\"department_link\":\"https://example.test/department\",\"position_name\":\"Преподаватель\","
                + "\"vacation\":null,\"start_vacation\":null,\"end_vacation\":null}],"
                + "\"powers\":[{\"dep_name\":\"Тестовое подразделение\",\"dep_link\":\"https://example.test/department\","
                + "\"power_name\":\"Тестовое полномочие\"}],\"levels\":{\"rank\":\"Тестовое звание\","
                + "\"degree\":\"Тестовая степень\"},\"education\":[],\"activities\":null,\"exchange_training\":false}";

        Personality personality = gson.fromJson(json, Personality.class);

        assertEquals(234567L, personality.getIsu());
        assertEquals("Тестовый Сотрудник", personality.getFio());
        assertEquals("male", personality.getGender());
        assertEquals("https://example.test/employee.jpg", personality.getPhotoUrl());
        assertEquals(1, personality.getContacts().size());
        Contact contact = personality.getContacts().get(0);
        assertEquals(Collections.singletonList("teacher@example.test"), contact.getContact());
        assertEquals("Электронная почта", contact.getContactAlias());
        assertTrue(personality.getRooms().isEmpty());
        assertEquals(1, personality.getPositions().size());
        Position position = personality.getPositions().get(0);
        assertEquals("Тестовое подразделение", position.getDepartmentName());
        assertEquals("https://example.test/department", position.getDepartmentLink());
        assertEquals("Преподаватель", position.getPositionName());
        assertTrue(personality.getEducation().isEmpty());
        assertFalse(personality.isExchangeTraining());
    }

    @Test
    void serviceProfileKeepsNullPhotoAndEmptyLists() {
        String json = "{\"isu\":345678,\"fio\":\"Тестовая Служебная Запись\",\"gender\":\"male\","
                + "\"photo\":null,\"contacts\":[],\"rooms\":[],\"positions\":[],\"powers\":[],"
                + "\"levels\":null,\"education\":[],\"activities\":null,\"exchange_training\":false}";

        Personality personality = gson.fromJson(json, Personality.class);

        assertEquals(345678L, personality.getIsu());
        assertNull(personality.getPhotoUrl());
        assertTrue(personality.getContacts().isEmpty());
        assertTrue(personality.getRooms().isEmpty());
        assertTrue(personality.getPositions().isEmpty());
        assertTrue(personality.getEducation().isEmpty());
        assertFalse(personality.isExchangeTraining());
    }
}
