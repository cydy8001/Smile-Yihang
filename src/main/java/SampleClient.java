import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.util.StopWatch;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SampleClient {

    public static void main(String[] theArgs) {

        // Create a FHIR client
        FhirContext fhirContext = FhirContext.forR4();
        IGenericClient client = fhirContext.newRestfulGenericClient("http://hapi.fhir.org/baseR4");
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
            ArrayList<String> firstNames = getFirstNames(entry);
            String firstName = firstNames.get(0);
            // Get last name
            ArrayList<String> lastNames = getLastNames(entry);
            String lastName = lastNames.get(0);
            // Get birth Date
            String birthDate = getBirthDate(entry);
            String[] info = {firstName, lastName, birthDate};
            result.put(resource, info);
        }
        // Sort by first name
        List<HashMap.Entry<Resource, String[]>> infoIds = new ArrayList<HashMap.Entry<Resource, String[]>>(result.entrySet());
        sortByFirstName(infoIds);
        // Print the result
        printResult(infoIds);


        // Intermediate

        ArrayList<String> familyNames = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        final long[] totalTime = new long[1];
        reggisterIClientInterceptor(client, stopWatch, totalTime);
        readFromTxtFile(familyNames);
        long firstTime = requestFirstTime(familyNames, client, totalTime);
        long secondTime = requestSecondTime(familyNames, client, totalTime);
        long thirdTime = requestThirdTime(familyNames, client, totalTime);
        System.out.println("The average response time for 3 times are " + firstTime + "ms "+ secondTime + "ms " + thirdTime + "ms");
    }

    static void sortByFirstName(List<Map.Entry<Resource, String[]>> infoIds) {
        infoIds.sort(new Comparator<HashMap.Entry<Resource, String[]>>() {
            public int compare(Map.Entry<Resource, String[]> o1, Map.Entry<Resource, String[]> o2) {
                return (o1.getValue()[0].compareTo(o2.getValue()[0]));
            }
        });
    }

    static long requestThirdTime(ArrayList<String> familyNames, IGenericClient client, long[] totalTime) {
        System.out.println("The Third time");
        requestCacheDisabled(familyNames, client);
        long thirdTime = totalTime[0] / familyNames.size();
        System.out.println("The average response time is " + totalTime[0] / familyNames.size() + "ms");
        return thirdTime;
    }

    static long requestSecondTime(ArrayList<String> familyNames, IGenericClient client, long[] totalTime) {
        System.out.println("The Second time");
        requestCacheEnabled(familyNames, client);
        long secondTime = totalTime[0] / familyNames.size();
        System.out.println("The average response time is " + totalTime[0] / familyNames.size() + "ms");
        totalTime[0] = 0;
        return secondTime;
    }

    static long requestFirstTime(ArrayList<String> familyNames, IGenericClient client, long[] totalTime) {
        System.out.println("The First time");
        requestCacheEnabled(familyNames, client);
        long firstTime = totalTime[0] / familyNames.size();
        System.out.println("The average response time is " + firstTime + "ms");
        totalTime[0] = 0;
        return firstTime;
    }

    static void readFromTxtFile(ArrayList<String> familyNames) {
        try {
            File file = new File("./src/main/resources/familyName.txt");
            Scanner input = new Scanner(file);
            while (input.hasNext()) {
                familyNames.add(input.nextLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void reggisterIClientInterceptor(IGenericClient client, StopWatch stopWatch, long[] totalTime) {
        client.registerInterceptor(new IClientInterceptor() {
            @Override
            public void interceptRequest(IHttpRequest iHttpRequest) {
                stopWatch.startTask(iHttpRequest.toString());
            }

            @Override
            public void interceptResponse(IHttpResponse iHttpResponse) throws IOException {
                stopWatch.endCurrentTask();
                totalTime[0] += stopWatch.getMillis();
                stopWatch.restart();
            }
        });
    }


    private static void requestCacheDisabled(ArrayList<String> familyNames, IGenericClient client) {
        for (String familyName : familyNames) {
                    client
                    .search()
                    .forResource("Patient")
                    .where(Patient.FAMILY.matches().value(familyName))
                    .returnBundle(Bundle.class)
                    .cacheControl(new CacheControlDirective().setNoCache(true))
                    .execute();
        }
    }

    private static void requestCacheEnabled(ArrayList<String> familyNames, IGenericClient client) {
        for (String familyName : familyNames) {
                    client
                    .search()
                    .forResource("Patient")
                    .where(Patient.FAMILY.matches().value(familyName))
                    .returnBundle(Bundle.class)
                    .execute();
        }
    }

    private static void printResult(List<Map.Entry<Resource, String[]>> infoIds) {
        for (int i = 0; i < infoIds.size(); i++) {
            String firstName = infoIds.get(i).getValue()[0];
            String lastName = infoIds.get(i).getValue()[1];
            String birthDate = infoIds.get(i).getValue()[2];
            System.out.println("Person " + (i + 1) + " :");
            System.out.println("The first name is " + firstName);
            System.out.println("The last name is " + lastName);
            if (birthDate == null) {
                System.out.println("No birth date is found");
            } else {
                System.out.println("The birth date is " + birthDate);
            }
        }
    }


    static String getBirthDate(Bundle.BundleEntryComponent entry) {
        List<Base> birthDateValues = entry.getResource().getChildByName("birthDate").getValues();
        boolean hasValues = !birthDateValues.isEmpty();
        if (hasValues) {
            return birthDateValues.get(0).dateTimeValue().asStringValue();
        }
        return null;
    }

    static ArrayList<String> getLastNames(Bundle.BundleEntryComponent entry) {
        ArrayList<String> result = new ArrayList<>();
        List<Base> nameValues = entry.getResource().getChildByName("name").getValues();
        for (Base nameValue : nameValues) {
            List<Base> familyValues = nameValue.getChildByName("family").getValues();
            for (Base familyValue : familyValues) {
                result.add(familyValue.primitiveValue());
            }
        }
        return result;
    }

    static ArrayList<String> getFirstNames(Bundle.BundleEntryComponent entry) {
        ArrayList<String> result = new ArrayList<>();
        List<Base> nameValues = entry.getResource().getChildByName("name").getValues();
        for (Base nameValue : nameValues) {
            List<Base> givenValues = nameValue.getChildByName("given").getValues();
            for (Base givenValue : givenValues) {
                result.add(givenValue.primitiveValue());
            }
        }

        return result;
    }
}
