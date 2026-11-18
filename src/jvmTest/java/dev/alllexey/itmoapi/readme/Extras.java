package dev.alllexey.itmoapi.readme;

import dev.alllexey.itmoapi.Blocking;
import dev.alllexey.itmoapi.blocking.BlockingMyItmo;
import dev.alllexey.itmoapi.core.MyItmoException;
import dev.alllexey.itmoapi.itmoid.FileTokenStorage;
import dev.alllexey.itmoapi.myitmo.schedule.Schedule;
import dev.alllexey.itmoapi.myitmo.sport.SportSignResult;
import kotlinx.datetime.ConvertersKt;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class Extras {
    // README javasession begin
    static BlockingMyItmo openSession(String username, String password) throws MyItmoException {
        return BlockingMyItmo.loginOrResume(username, password, new FileTokenStorage(Path.of("itmo-tokens.json")));
    }
    // README javasession end

    // README javaerrors begin
    static List<Schedule> weekOrNull(BlockingMyItmo itmo, LocalDate monday) {
        try {
            return itmo.getSchedule().getPersonalSchedule(monday, monday.plusDays(6));
        } catch (MyItmoException.Auth e) {
            return null; // сессия закончилась: нужен новый вход
        } catch (MyItmoException e) {
            return null; // Api, Http, Network, Decode, Empty
        }
    }
    // README javaerrors end

    // README javasport begin
    static String book(BlockingMyItmo itmo, long lessonId) {
        SportSignResult result = itmo.getSport().signInLesson(lessonId);
        if (result instanceof SportSignResult.Booked) return "записан";
        if (result instanceof SportSignResult.Full) return "мест нет, можно попробовать позже";
        if (result instanceof SportSignResult.Rejected) {
            return "отказ: код " + ((SportSignResult.Rejected) result).getRefusal().getErrorCode();
        }
        return "решения нет, повторить позже";
    }
    // README javasport end

    // README javadates begin
    static LocalDate dateOf(Schedule day) {
        return ConvertersKt.toJavaLocalDate(day.getDate());
    }
    // README javadates end

    // README javacall begin
    static String refreshNow(BlockingMyItmo itmo) throws Exception {
        return Blocking.call(c -> itmo.getItmo().getClient().getTokens().forceRefresh(c));
    }
    // README javacall end
}
