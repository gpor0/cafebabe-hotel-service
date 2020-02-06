package com.github.gpor0.resources;

import com.github.gpor0.business.HotelService;
import com.github.gpor0.business.model.Reservation;
import com.kumuluz.ee.logs.LogManager;
import com.kumuluz.ee.logs.Logger;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.gpor0.business.HotelService.MAX_ROOMS;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path("/hotels")
@RequestScoped
public class HotelResource {


    private static final Logger LOG = LogManager.getLogger("HotelService");

    @Inject
    private HotelService hotelService;

    @GET
    @Path("/{hotelName}")
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response reserve(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, @PathParam("hotelName") String hotelName, @QueryParam("customer") String customer) {
        LOG.info("Received reservation for hotel {} and customer {}", hotelName, customer);

        try {
            hotelName = hotelService.reserve(lraId, customer, hotelName);
            LOG.info("Hotel {} is available", hotelName);
            return Response.ok(hotelName).build();
        } catch (Exception e) {
            LOG.error("Hotel {} is NOT available", hotelName);
            return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/")
    public Response getRooms() {
        Map<String, Map<URI, String>> reservations = hotelService.getReservations();

        List<Reservation> result = reservations.entrySet().stream().map(e -> new Reservation(e.getKey(),
                e.getValue().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList()),
                MAX_ROOMS, MAX_ROOMS - e.getValue().size())
        ).collect(Collectors.toList());

        return Response.ok(result).build();
    }

    @Complete
    @Path("/complete")
    @PUT
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        AbstractMap.SimpleEntry<String, String> reservation = hotelService.getReservation(lraId);

        if (reservation != null) {
            LOG.info("Reservation for hotel {} and customer {} confirmed", reservation.getKey(), reservation.getValue());
        }

        return Response.ok(ParticipantStatus.Completed.name()).build();
    }

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensate(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        AbstractMap.SimpleEntry<String, String> reservation = hotelService.getReservation(lraId);

        if (reservation != null) {
            LOG.info("StartingCompensation procedure for hotel {} and customer {}", reservation.getKey(), reservation.getValue());

            hotelService.rollback(lraId);

            LOG.info("Compensation procedure for hotel {} and customer {} completed", reservation.getKey(), reservation.getValue());

        }
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }

}
