package hu.progmasters.servicebooker.controller;

public class LogMessages {
    public static final String LOG_SAVE = "POST request on {}, body: {}";
    public static final String LOG_FINDBYID = "GET request on {}/{}";
    public static final String LOG_UPDATE = "PUT request on {}/{}, body: {}";
    public static final String LOG_DELETE = "DELETE request on {}/{}";
    public static final String LOG_GET = "GET request on {}";

    public static final String LOG_SAVE_SUB = "POST request on {}/{}{}, body: {}";
    public static final String LOG_FINDBYID_SUB = "GET request on {}/{}{}/{}";
    public static final String LOG_UPDATE_SUB = "PUT request on {}/{}{}/{}, body: {}";
    public static final String LOG_DELETE_SUB = "DELETE request on {}/{}{}/{}";
    public static final String LOG_GET_SUB = "GET request on {}/{}{}";

    public static final String LOG_RESPONSE = "HTTP status {}, response: {}";
}
