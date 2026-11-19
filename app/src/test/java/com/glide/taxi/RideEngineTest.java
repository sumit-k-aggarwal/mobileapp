package com.glide.taxi;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.glide.taxi.RideEngine.*;

public class RideEngineTest {
    private static final long NOW = 1_800_000_000_000L;

    private Trip ride() {
        return book(place("market"), place("airport"), Service.EVERYDAY, "Demo card", 0, NOW);
    }

    @Test public void faresSumExactlyAndIncreaseWithCapacity() {
        int previous = 0;
        for (Service service : Service.values()) {
            Quote quote = quote(place("market"), place("airport"), service);
            assertEquals(quote.baseCents() + quote.distanceCents() + quote.timeCents()
                + quote.bookingCents(), quote.totalCents());
            assertTrue(quote.totalCents() > previous);
            assertTrue(quote.kilometers() > 20);
            previous = quote.totalCents();
        }
    }

    @Test public void shortRideHasNonzeroFareAndDuration() {
        Quote quote = quote(place("market"), place("ferry"), Service.EVERYDAY);
        assertTrue(quote.kilometers() >= 0.8);
        assertTrue(quote.minutes() >= 4);
        assertEquals("$12.05", money(1205));
    }

    @Test(expected = IllegalArgumentException.class)
    public void samePickupAndDestinationAreRejected() {
        quote(place("market"), place("market"), Service.EVERYDAY);
    }

    @Test public void simulationTransitionsOnlyAtStageDeadline() {
        Trip trip = ride();
        assertFalse(tick(trip, NOW + 4999));
        assertTrue(tick(trip, NOW + 5000));
        assertEquals(Stage.ARRIVING, trip.stage);
        assertFalse(tick(trip, NOW + 19_999));
        assertTrue(tick(trip, NOW + 20_000));
        assertEquals(Stage.IN_PROGRESS, trip.stage);
        assertTrue(tick(trip, NOW + 55_000));
        assertEquals(Stage.COMPLETED, trip.stage);
        assertFalse(tick(trip, NOW + 90_000));
    }

    @Test public void scheduleWaitsUntilRequestedTime() {
        Trip trip = book(place("market"), place("union"), Service.COMFORT, "Cash",
            NOW + 600_000, NOW);
        assertEquals(Stage.SCHEDULED, trip.stage);
        assertFalse(tick(trip, NOW + 599_999));
        assertTrue(tick(trip, NOW + 600_000));
        assertEquals(Stage.SEARCHING, trip.stage);
        assertEquals("Cash", trip.payment);
    }

    @Test(expected = IllegalArgumentException.class)
    public void scheduleTooSoonIsRejected() {
        book(place("market"), place("union"), Service.EVERYDAY, "Cash", NOW + 299_999, NOW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void scheduleTooFarAheadIsRejected() {
        book(place("market"), place("union"), Service.EVERYDAY, "Cash",
            NOW + 31L * 24 * 60 * 60_000, NOW);
    }

    @Test public void cancellationsRemainTerminal() {
        Trip trip = ride();
        cancel(trip, NOW + 100);
        assertEquals(Stage.CANCELLED, trip.stage);
        assertFalse(tick(trip, NOW + 100_000));
    }

    @Test(expected = IllegalStateException.class)
    public void rideInProgressCannotBeCancelled() {
        Trip trip = ride();
        advance(trip, NOW);
        advance(trip, NOW);
        cancel(trip, NOW);
    }

    @Test(expected = IllegalStateException.class)
    public void completedRideCannotBeAdvancedAgain() {
        Trip trip = ride();
        advance(trip, NOW);
        advance(trip, NOW);
        advance(trip, NOW);
        advance(trip, NOW);
    }
}
