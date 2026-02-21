package org.example.model;

import java.util.*;

public class Person {
    public String id;
    public String firstName;
    public String lastName;
    public String gender;

    public Set<String> spouses = new HashSet<>();
    public Set<String> parents = new HashSet<>();
    public Set<String> children = new HashSet<>();
    public Set<String> brothers = new HashSet<>();
    public Set<String> sisters = new HashSet<>();

    public String fullName() {
        return (firstName == null ? "" : firstName) + " " +
                (lastName == null ? "" : lastName).trim();
    }
}
