package ru.ifmo.rain.sviridov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {
    private static final Comparator<Student> STUDENT_USELESS_COMPARATOR = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return reduceToGroupList(collection, STUDENT_USELESS_COMPARATOR);
    }

    private Stream<Map.Entry<String, List<Student>>> groupToStudentsStream(Stream<Student> students) {
        return students
                .collect(Collectors.groupingBy(Student::getGroup, HashMap::new, Collectors.toList()))
                .entrySet()
                .stream();

    }

    private List<Group> reduceToGroupList(Collection<Student> collection, Comparator<Student> comparator) {
        return groupToStudentsStream(collection
                .stream()
                .sorted(comparator))
                .map(entry -> new Group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Group::getName, String::compareTo))
                .collect(Collectors.toList());
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return reduceToGroupList(collection, Comparator.comparing(Student::getId, Integer::compareTo));
    }

    private String getLargestGroupByComparator(Stream<Map.Entry<String, List<Student>>> stream, Comparator<List<Student>> comparator) {
        return stream
                .max(Map.Entry.<String, List<Student>>comparingByValue(comparator)
                .thenComparing(Map.Entry.comparingByKey(Collections.reverseOrder(String::compareTo))))
                .map(Map.Entry::getKey)
                .orElse("");
    }

    @Override
    public String getLargestGroup(Collection<Student> collection) {
        return getLargestGroupByComparator(groupToStudentsStream(collection.stream()), Comparator.comparing(List::size));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> collection) {
        return getLargestGroupByComparator(groupToStudentsStream(collection.stream()), Comparator.comparingInt(list -> getDistinctFirstNames(list).size()));
    }

    private List<String> getFieldsOfStudents(List<Student> list, Function<? super Student, ? extends String> function) {
        return list.stream()
                .map(function)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> list) {
        return getFieldsOfStudents(list, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return getFieldsOfStudents(list, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> list) {
        return getFieldsOfStudents(list, Student::getGroup);
    }

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return getFieldsOfStudents(list, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return new TreeSet<>(getFieldsOfStudents(list, Student::getFirstName));
    }

    @Override
    public String getMinStudentFirstName(List<Student> list) {
        return list.stream().min(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return sortStudentsByComparator(collection, Student::compareTo);
    }

    private List<Student> sortStudentsByComparator(Collection<Student> collection, Comparator<Student> comparator) {
        return collection.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return sortStudentsByComparator(collection, STUDENT_USELESS_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return findStudentsByPredicate(collection, (student -> student.getFirstName().equals(s)));
    }

    private List<Student> findStudentsByPredicate(Collection<Student> collection, Predicate<? super Student> predicate) {
        return collection.stream().filter(predicate).sorted(STUDENT_USELESS_COMPARATOR).collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return findStudentsByPredicate(collection, (student -> student.getLastName().equals(s)));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, String s) {
        return findStudentsByPredicate(collection, (student -> student.getGroup().equals(s)));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, String s) {
        return findStudentsByGroup(collection, s)
                .stream()
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }
}
