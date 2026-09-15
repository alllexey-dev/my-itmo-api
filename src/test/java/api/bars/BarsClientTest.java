package api.bars;

import api.bars.model.Approval;
import api.bars.model.StudentJournal;
import api.bars.model.Term;
import api.bars.model.User;
import api.bars.utils.BarsApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BarsClientTest {
    private final MockWebServer server = new MockWebServer();
    private Bars bars;

    @BeforeEach
    void start() throws IOException {
        server.start();
        bars = new Bars(new BarsConfiguration.Default() {
            @Override
            public String getHost() {
                return server.getHostName();
            }

            @Override
            public String getRestUrl() {
                return server.url("/backend/rest/").toString();
            }
        });
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    private static final String USER = "{\"id\":1,\"login\":\"123456\",\"first_name\":\"\",\"middle_name\":\"\",\"last_name\":\"\","
            + "\"user_roles\":[{\"id\":2,\"name\":\"Обучающийся\"}],\"selected_role\":{\"id\":2,\"name\":\"Обучающийся\",\"locked\":false,\"selected\":true},"
            + "\"selected_year\":\"2026/2027\",\"selected_term\":1,\"personal_config\":[],\"can_change_user\":false,\"restricted_to_have_read_only_access\":false}";

    @Test
    void loginStoresTheAuthorizationHeaderAndRequestsCarryIt() throws Exception {
        server.enqueue(new MockResponse().setHeader("authorization", "Bearer synthetic-token"));
        server.enqueue(new MockResponse().setBody(USER));
        bars.login("synthetic&code");
        assertEquals("Bearer synthetic-token", bars.getStorage().getAuthorization());
        RecordedRequest login = server.takeRequest();
        assertEquals("/backend/rest/login?code=synthetic%26code&customRedirectUri=https%3A%2F%2F" + server.getHostName() + "%2Frest%2Flogin", login.getPath());
        assertNull(login.getHeader("Authorization"));
        User user = bars.getCurrentUser();
        assertEquals("123456", user.getLogin());
        assertEquals(Term.AUTUMN, user.getSelectedTermValue());
        assertEquals("Bearer synthetic-token", server.takeRequest().getHeader("Authorization"));
    }

    @Test
    void unauthorizedWithoutCodeSupplierIsAnException() {
        bars.getStorage().setAuthorization("Bearer synthetic-token");
        server.enqueue(new MockResponse().setResponseCode(401));
        BarsApiException error = assertThrows(BarsApiException.class, () -> bars.getCurrentUser());
        assertTrue(error.isUnauthorized());
        assertThrows(BarsApiException.class, () -> new Bars().getCurrentUser());
    }

    @Test
    void expiredSessionIsRenewedOnceThroughTheCodeSupplierAndRetried() throws Exception {
        bars.getStorage().setAuthorization("Bearer synthetic-expired-token");
        List<String> states = new ArrayList<>();
        bars.setCodeSupplier(state -> { states.add(state); return "fresh-code"; });
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setHeader("authorization", "Bearer synthetic-fresh-token"));
        server.enqueue(new MockResponse().setBody(USER));
        assertEquals("123456", bars.getCurrentUser().getLogin());
        assertEquals(1, states.size());
        assertEquals("Bearer synthetic-expired-token", server.takeRequest().getHeader("Authorization"));
        assertTrue(server.takeRequest().getPath().contains("code=fresh-code"));
        assertEquals("Bearer synthetic-fresh-token", server.takeRequest().getHeader("Authorization"));
        assertEquals("Bearer synthetic-fresh-token", bars.getStorage().getAuthorization());
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setHeader("authorization", "Bearer synthetic-fresh-token-2"));
        server.enqueue(new MockResponse().setResponseCode(401));
        assertTrue(assertThrows(BarsApiException.class, () -> bars.getCurrentUser()).isUnauthorized());
    }

    @Test
    void supplierWithoutCodeLeavesTheSessionUnauthorized() {
        bars.setCodeSupplier(state -> null);
        assertTrue(assertThrows(BarsApiException.class, () -> bars.getCurrentUser()).isUnauthorized());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void selectPeriodWritesOnlyDifferencesAndVerifiesThem() throws Exception {
        bars.getStorage().setAuthorization("Bearer synthetic-token");
        server.enqueue(new MockResponse().setBody(USER));
        assertEquals("2026/2027", bars.selectPeriod("2026/2027", Term.AUTUMN).getSelectedYear());
        assertEquals(1, server.getRequestCount());
        server.takeRequest();
        server.enqueue(new MockResponse().setBody(USER));
        server.enqueue(new MockResponse().setBody("{\"id\":9,\"name\":\"current_year\",\"value\":\"2025/2026\"}"));
        server.enqueue(new MockResponse().setBody("{\"id\":9,\"name\":\"current_term\",\"value\":\"0\"}"));
        server.enqueue(new MockResponse().setBody(USER.replace("2026/2027", "2025/2026").replace("\"selected_term\":1", "\"selected_term\":0")));
        bars.selectPeriod("2025/2026", Term.SPRING);
        server.takeRequest();
        RecordedRequest year = server.takeRequest();
        assertEquals("POST", year.getMethod());
        assertEquals("/backend/rest/config/personal", year.getPath());
        assertEquals("{\"name\":\"current_year\",\"value\":\"2025/2026\"}", year.getBody().readUtf8());
        assertEquals("{\"name\":\"current_term\",\"value\":\"0\"}", server.takeRequest().getBody().readUtf8());
        server.enqueue(new MockResponse().setBody(USER));
        server.enqueue(new MockResponse().setBody("{\"id\":9,\"name\":\"current_term\",\"value\":\"0\"}"));
        server.enqueue(new MockResponse().setBody(USER));
        assertThrows(BarsApiException.class, () -> bars.selectPeriod("2026/2027", Term.SPRING));
    }

    @Test
    void journalPathEncodesIdentifierAndParsesScores() throws Exception {
        bars.getStorage().setAuthorization("Bearer synthetic-token");
        server.enqueue(new MockResponse().setBody("{\"students\":[{\"student_id\":1,\"student_login\":\"123456\",\"student_name\":\"\","
                + "\"marks\":{\"regular\":[{\"id\":5,\"checkpoint_id\":6,\"checkpoint_plan_id\":8,\"mark\":7.5,\"type\":\"current\",\"is_absent\":false}],"
                + "\"additional\":{\"id\":7,\"checkpoint_id\":null,\"checkpoint_plan_id\":8,\"mark\":2.0,\"type\":\"current\",\"is_absent\":false},"
                + "\"regularSum\":7.5,\"total\":9.5,\"active_approvals\":[{\"id\":1,\"attempt\":2,\"checkpoint_plan_id\":8,\"student_login\":\"123456\","
                + "\"marks_sum\":9.5,\"mark_string\":\"Удвл., E\",\"is_active\":true,\"is_invalid\":false,\"is_absent\":false,\"course\":false}]}}],"
                + "\"headers\":{\"plan\":{\"id\":8,\"year\":\"2025/2026\",\"terms\":[2],\"discipline\":{\"id\":90,\"name\":\"Предмет\"},"
                + "\"regular_checkpoints\":[{\"id\":6,\"name\":\"Работа\",\"type\":\"Тест\",\"min_grade\":1.0,\"max_grade\":10.0,\"key\":true,\"sub_checkpoints\":[]}],"
                + "\"final_checkpoint\":{\"id\":9,\"name\":null,\"type\":\"Экзамен\",\"min_grade\":12.0,\"max_grade\":20.0,\"key\":false,\"sub_checkpoints\":[]},"
                + "\"has_course_project\":false},\"type\":\"flow\",\"identifier\":\"a/b\",\"name\":\"Поток\"}}"));
        StudentJournal journal = bars.execute(bars.getApi().getStudentJournal(8, "flow", "a/b"));
        assertEquals("/backend/rest/marks/8/flow/a%2Fb/student", server.takeRequest().getPath());
        assertNull(journal.getStudents().get(0).getMarks().getAdditional().getCheckpointId());
        assertTrue(journal.getStudents().get(0).getMarks().hasAnyMark());
        assertNull(journal.getHeaders().getPlan().getFinalCheckpoint().getName());
        assertEquals("3/E", journal.getStudents().get(0).getMarks().getActiveApprovals().get(0).getGradeCode());
        StudentJournal empty = bars.getGson().fromJson("{\"students\":[{\"marks\":{\"regular\":[],\"total\":0.0,\"active_approvals\":[]}}]}", StudentJournal.class);
        assertFalse(empty.getStudents().get(0).getMarks().hasAnyMark());
        assertNull(empty.getStudents().get(0).getMarks().getFinalMark());
    }

    @Test
    void gradeCodesFollowTheMyItmoFormat() {
        Approval approval = new Approval();
        String[][] cases = {{"Отл., A", "5/A"}, {"Хор., C", "4/C"}, {"Удвл., E", "3/E"}, {"Неуд., FX", "2/FX"},
                {"Зачет", "Зачет"}, {"Незачет", "Незачет"}, {"Удвл.", "Удвл."}, {" Отл., B ", "5/B"}};
        for (String[] item : cases) {
            approval.setMarkString(item[0]);
            assertEquals(item[1], approval.getGradeCode(), item[0]);
        }
        approval.setMarkString(null);
        assertNull(approval.getGradeCode());
    }

    @Test
    void rotatedHeaderReplacesTheStoredSession() {
        bars.getStorage().setAuthorization("Bearer synthetic-token");
        server.enqueue(new MockResponse().setBody(USER).setHeader("authorization", "Bearer synthetic-rotated-token"));
        bars.getCurrentUser();
        assertEquals("Bearer synthetic-rotated-token", bars.getStorage().getAuthorization());
    }
}
