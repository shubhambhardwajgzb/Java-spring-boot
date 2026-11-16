
package com.example.demo.controller;

import com.example.demo.model.Employee;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private Map<Integer, Employee> employees = new HashMap<>();

    // POST - Add Employee
    @PostMapping
    public Employee addEmployee(@RequestBody Employee emp) {
        employees.put(emp.getId(), emp);
        return emp;
    }

    // GET - All Employees
    @GetMapping
    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employees.values());
    }

    // GET - Employee By ID
    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable int id) {
        return employees.get(id);
    }

    // PUT - Update Employee
    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable int id,
                                   @RequestBody Employee emp) {
        if (!employees.containsKey(id)) {
            return null;
        }

        emp.setId(id);
        employees.put(id, emp);
        return emp;
    }

    // DELETE - Delete Employee
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(@PathVariable int id) {
        if (!employees.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }

        employees.remove(id);
        return ResponseEntity.ok("Employee deleted successfully");
    }
}
