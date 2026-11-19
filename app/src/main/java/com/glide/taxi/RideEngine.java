package com.glide.taxi;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class RideEngine {
    private RideEngine() {}

    public record Place(String id, String name, String address, double lat, double lng) {
        @Override public String toString() { return name; }
    }

    public enum Service {
        EVERYDAY("Glide", "Everyday rides, done right", 4, 1.0, 3),
        COMFORT("Comfort", "More room. A little more calm.", 4, 1.38, 5),
        XL("Glide XL", "Room for your whole crew", 6, 1.78, 7);

        public final String title;
        public final String description;
        public final int seats;
        public final double multiplier;
        public final int arrivalMinutes;

        Service(String title, String description, int seats, double multiplier, int arrivalMinutes) {
            this.title = title;
            this.description = description;
            this.seats = seats;
            this.multiplier = multiplier;
            this.arrivalMinutes = arrivalMinutes;
        }
    }

    public enum Stage { SCHEDULED, SEARCHING, ARRIVING, IN_PROGRESS, COMPLETED, CANCELLED }

    public static final List<Place> PLACES = Arrays.asList(
        new Place("market", "Market Street", "1 Market St, San Francisco", 37.7936, -122.3958),
        new Place("ferry", "Ferry Building", "1 Ferry Building, Embarcadero", 37.7955, -122.3937),
        new Place("airport", "San Francisco Airport", "SFO, Departures level", 37.6213, -122.3790),
        new Place("park", "Golden Gate Park", "Music Concourse, San Francisco", 37.7694, -122.4862),
        new Place("union", "Union Square", "333 Post St, San Francisco", 37.7880, -122.4075),
        new Place("pier", "Pier 39", "Beach St & The Embarcadero", 37.8087, -122.4098),
        new Place("home", "Pacific Heights", "2400 California St, San Francisco", 37.7891, -122.4340),
        new Place("work", "Design District", "101 Henry Adams St, San Francisco", 37.7684, -122.4034)
    );

    public static Place place(String id) {
        return PLACES.stream().filter(p -> p.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown demo location: " + id));
    }

    public record Quote(double kilometers, int minutes, int baseCents, int distanceCents,
                        int timeCents, int bookingCents, int totalCents) {}

    public static Quote quote(Place pickup, Place destination, Service service) {
        if (pickup.id().equals(destination.id())) {
            throw new IllegalArgumentException("Choose a destination different from your pickup.");
        }
        double dLat = Math.toRadians(destination.lat() - pickup.lat());
        double dLng = Math.toRadians(destination.lng() - pickup.lng());
        double a = Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(Math.toRadians(pickup.lat())) * Math.cos(Math.toRadians(destination.lat()))
            * Math.pow(Math.sin(dLng / 2), 2);
        double km = Math.max(0.8, 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 1.3);
        int minutes = Math.max(4, (int) Math.ceil(km / 0.42) + 3);
        int base = (int) Math.round(300 * service.multiplier);
        int distance = (int) Math.round(km * 125 * service.multiplier);
        int time = (int) Math.round(minutes * 22 * service.multiplier);
        int booking = 175;
        return new Quote(km, minutes, base, distance, time, booking, base + distance + time + booking);
    }

    public static String money(int cents) {
        return String.format(Locale.US, "$%.2f", cents / 100.0);
    }

    public static final class Trip {
        public String id;
        public String pickupId;
        public String destinationId;
        public Service service;
        public String payment;
        public Stage stage;
        public int fareCents;
        public long createdAt;
        public long scheduledAt;
        public long stageAt;
        public int rating;

        public Place pickup() { return place(pickupId); }
        public Place destination() { return place(destinationId); }
    }

    public static Trip book(Place pickup, Place destination, Service service, String payment,
                            long scheduledAt, long now) {
        if (!"Demo card".equals(payment) && !"Cash".equals(payment)) {
            throw new IllegalArgumentException("Choose a supported demo payment method.");
        }
        if (scheduledAt != 0 && scheduledAt < now + 5 * 60_000L) {
            throw new IllegalArgumentException("Schedule at least 5 minutes from now.");
        }
        if (scheduledAt > now + 30L * 24 * 60 * 60_000) {
            throw new IllegalArgumentException("Schedule within the next 30 days.");
        }
        Trip trip = new Trip();
        trip.id = "GL-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US);
        trip.pickupId = pickup.id();
        trip.destinationId = destination.id();
        trip.service = service;
        trip.payment = payment;
        trip.fareCents = quote(pickup, destination, service).totalCents();
        trip.createdAt = now;
        trip.scheduledAt = scheduledAt;
        trip.stageAt = now;
        trip.stage = scheduledAt == 0 ? Stage.SEARCHING : Stage.SCHEDULED;
        return trip;
    }

    public static void advance(Trip trip, long now) {
        trip.stage = switch (trip.stage) {
            case SCHEDULED -> Stage.SEARCHING;
            case SEARCHING -> Stage.ARRIVING;
            case ARRIVING -> Stage.IN_PROGRESS;
            case IN_PROGRESS -> Stage.COMPLETED;
            default -> throw new IllegalStateException("This ride has already ended.");
        };
        trip.stageAt = now;
    }

    public static boolean tick(Trip trip, long now) {
        long delay = switch (trip.stage) {
            case SEARCHING -> 5_000;
            case ARRIVING -> 15_000;
            case IN_PROGRESS -> 35_000;
            default -> Long.MAX_VALUE;
        };
        if (trip.stage == Stage.SCHEDULED && now >= trip.scheduledAt) {
            advance(trip, now);
            return true;
        }
        if (now >= trip.stageAt && now - trip.stageAt >= delay) {
            advance(trip, now);
            return true;
        }
        return false;
    }

    public static void cancel(Trip trip, long now) {
        if (trip.stage != Stage.SCHEDULED && trip.stage != Stage.SEARCHING && trip.stage != Stage.ARRIVING) {
            throw new IllegalStateException("Only a ride that has not started can be cancelled.");
        }
        trip.stage = Stage.CANCELLED;
        trip.stageAt = now;
    }
}
