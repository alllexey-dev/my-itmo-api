package dev.alllexey.itmoapi.readme;

// README java begin
import dev.alllexey.itmoapi.blocking.BlockingMyItmo;
import dev.alllexey.itmoapi.myitmo.schedule.Lesson;
import dev.alllexey.itmoapi.myitmo.schedule.Schedule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        try (BlockingMyItmo itmo = BlockingMyItmo.login("my_cool_id", "my_strong_password")) {
            LocalDate today = LocalDate.now(ZoneId.of("Europe/Moscow"));
            List<Schedule> week = itmo.getSchedule().getPersonalSchedule(today, today.plusDays(6));
            for (Schedule day : week) {
                System.out.println(day.getDate() + ":");
                for (Lesson lesson : day.getLessons()) {
                    System.out.println("  " + lesson.getTimeStart() + " " + lesson.getSubject());
                }
            }
        }
    }
}
// README java end
