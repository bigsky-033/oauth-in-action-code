package org.oauthinaction.ch03ex02.completed;

import org.oauthinaction.ch03ex02.Exercise;

import java.net.URI;

public final class CompletedExercise {
    public static void main(String[] args) {
        Exercise.start(new CompletedClient(URI.create("http://localhost:9001/"),
                URI.create("http://localhost:9002/"), URI.create("http://localhost:9000/callback")));
    }
}
