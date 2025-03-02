package io.github.zeront4e.gdl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ObjectCloneUtilTest {
    private static class TestPerson {
        private String name;
        private int age;
        private List<String> hobbies;

        public TestPerson() {}

        public TestPerson(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<String> getHobbies() {
            return hobbies;
        }

        public void setHobbies(List<String> hobbies) {
            this.hobbies = hobbies;
        }
    }

    @Test
    public void shouldCreateDeepCloneWithSeparateReferences() throws Exception {
        //Create a test object with nested structure.

        TestPerson originalPerson = new TestPerson("John Doe", 30);

        List<String> hobbies = new ArrayList<>();
        hobbies.add("Reading");
        hobbies.add("Coding");

        originalPerson.setHobbies(hobbies);
        
        //Clone the object.

        TestPerson clonedPerson = ObjectCloneUtil.deepCloneObject(originalPerson);
        
        //Verify the clone has the same values.

        assertEquals(originalPerson.getName(), clonedPerson.getName());
        assertEquals(originalPerson.getAge(), clonedPerson.getAge());
        assertEquals(originalPerson.getHobbies(), clonedPerson.getHobbies());
        
        //Verify the objects are different instances.

        assertNotSame(originalPerson, clonedPerson);
        
        //Verify nested objects are also different instances.

        assertNotSame(originalPerson.getHobbies(), clonedPerson.getHobbies());
        
        //Modify the original and verify the clone remains unchanged.

        originalPerson.setName("Jane Doe");
        originalPerson.getHobbies().add("Swimming");
        
        assertEquals("John Doe", clonedPerson.getName());
        assertEquals(2, clonedPerson.getHobbies().size());
        assertFalse(clonedPerson.getHobbies().contains("Swimming"));
    }


}