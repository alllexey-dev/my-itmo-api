package dev.alllexey.itmoapi.readme;

// README java begin
import dev.alllexey.itmoapi.Blocking;
import dev.alllexey.itmoapi.MyItmo;
import dev.alllexey.itmoapi.core.DataResponse;
import dev.alllexey.itmoapi.myitmo.schedule.Schedule;
import kotlinx.datetime.LocalDate;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (MyItmo itmo = Blocking.call(c -> MyItmo.login("my_cool_id", "my_strong_password", c))) {
            LocalDate from = new LocalDate(2026, 1, 5);
            LocalDate to = new LocalDate(2026, 1, 11);
            DataResponse<List<Schedule>> week = Blocking.call(c -> itmo.getSchedule().getPersonalSchedule(from, to, c));
            for (Schedule day : week.getData()) {
                System.out.println(day.getDate() + ": " + day.getLessons().size() + " пар");
            }
        }
    }
}
// README java end
