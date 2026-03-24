package com.impostorgame.auth.dto;

public record GuestRequest(
        String displayName  // optional — if null a random name is generated
) {}
