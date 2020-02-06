package com.github.gpor0.business;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class HotelService {

    public static final Integer MAX_ROOMS = 10;
    private static final Map<String, Map<URI, String>> RESERVATIONS = new HashMap<>();

    public String reserve(URI lraId, String customer, String hotelName) {

        hotelName = hotelName.toUpperCase();

        if (!RESERVATIONS.containsKey(hotelName)) {
            RESERVATIONS.put(hotelName, new HashMap<>());
        }

        Map<URI, String> hotelReservations = RESERVATIONS.get(hotelName);

        if (hotelReservations.size() == MAX_ROOMS) {
            throw new RuntimeException("No more rooms available on hotel " + hotelName);
        }

        hotelReservations.put(lraId, customer);
        return hotelName;

    }

    public AbstractMap.SimpleEntry<String, String> getReservation(URI lraId) {

        for (Map.Entry<String, Map<URI, String>> e : RESERVATIONS.entrySet()) {
            Map<URI, String> hotelReservations = e.getValue();
            if (hotelReservations.containsKey(lraId)) {
                String guestName = hotelReservations.get(lraId);
                return new AbstractMap.SimpleEntry<>(e.getKey(), guestName);
            }
        }

        return null;

    }

    public void rollback(URI lraId) {
        for (Map.Entry<String, Map<URI, String>> e : RESERVATIONS.entrySet()) {
            Map<URI, String> hotelReservations = e.getValue();
            if (hotelReservations.containsKey(lraId)) {
                hotelReservations.remove(lraId);
                RESERVATIONS.put(e.getKey(), hotelReservations);
                break;
            }
        }
    }

    public Map<String, Map<URI, String>> getReservations() {
        return RESERVATIONS;
    }
}
