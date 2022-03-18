
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.util.StopWatch;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class JTest {
    ArrayList<String> familyNames = new ArrayList<>();
    StopWatch stopWatch = new StopWatch();
    final long[] totalTime = new long[1];
    IGenericClient client;
    List<HashMap.Entry<Resource, String[]>> infoIds;
    @BeforeTest
    public void beforeTest() throws FileNotFoundException {
        FhirContext fhirContext = FhirContext.forR4();
        this.client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor(false);
        client.registerInterceptor(loggingInterceptor);

        // Search for Patient resources
        Bundle response = client
                .search()
                .forResource("Patient")
                .where(Patient.FAMILY.matches().value("SMITH"))
                .returnBundle(Bundle.class)
                .execute();
        List<Bundle.BundleEntryComponent> entries = response.getEntry();

        HashMap<Resource, String[]> result = new HashMap<>();
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            // Get first name
            ArrayList<String> firstNames = SampleClient.getFirstNames(entry);
            String firstName = firstNames.get(0);
            // Get last name
            ArrayList<String> lastNames = SampleClient.getLastNames(entry);
            String lastName = lastNames.get(0);
            // Get birth Date
            String birthDate = SampleClient.getBirthDate(entry);
            String[] info = {firstName, lastName, birthDate};
            result.put(resource, info);
        }
        this.infoIds = new ArrayList<HashMap.Entry<Resource, String[]>>(result.entrySet());

        SampleClient.reggisterIClientInterceptor(this.client, this.stopWatch, this.totalTime);

    }

    @Test
    public void getFamilyNames() throws FileNotFoundException {
        SampleClient.readFromTxtFile(familyNames);
        assertEquals(20,familyNames.size());
    }
    @Test
    public void testloopTime(){
        long firstTime = SampleClient.requestFirstTime(familyNames, client, totalTime);
        long secondTime = SampleClient.requestSecondTime(familyNames, client, totalTime);
        long thirdTime = SampleClient.requestThirdTime(familyNames, client, totalTime);
        boolean expected = secondTime < (firstTime + thirdTime)/2;
        assertTrue(expected);
    }
    @Test
    public void testSorted(){
        SampleClient.sortByFirstName(infoIds);
        boolean flag = true;
        for (int i = 0; i < infoIds.size()-1; i++) {
            String firstName1 = infoIds.get(i).getValue()[0];
            String firstName2 = infoIds.get(i+1).getValue()[0];
            if (firstName1.compareTo(firstName2) > 0){
                flag = false;
                break;
            }
        }
        assertTrue(flag);
    }
}
