package hu.progmasters.servicebooker.exceptionhandling.booking;

import lombok.Getter;

@Getter
public class BookingNotFoundException extends RuntimeException {

    private final int id;

    public BookingNotFoundException(int id) {
        super(defaultMessage(id));
        this.id = id;
    }

    private static String defaultMessage(int id) {
        return String.format("booking with id %d not found", id);
    }
}
