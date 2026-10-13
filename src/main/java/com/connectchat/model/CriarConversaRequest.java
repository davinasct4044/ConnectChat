package com.connectchat.model;

public record CriarConversaRequest(
        Long usuario1Id,
        Long usuario2Id
) {

}