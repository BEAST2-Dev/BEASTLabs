package test.beastlabs;

import beast.base.evolution.datatype.DataType;
import beast.pkgmgmt.BEASTClassLoader;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceDiscoveryTest {

    @Test
    public void testDataTypeDiscovery() {
        System.out.println("Boot layer modules:");
        ModuleLayer.boot().modules().stream()
            .map(Module::getName)
            .sorted()
            .forEach(n -> System.out.println("  " + n));

        System.out.println("\nLooking for DataType providers...");
        Set<String> dataTypes = BEASTClassLoader.loadService(DataType.class);
        System.out.println("Found: " + dataTypes);

        System.out.println("\nBEAST_PACKAGE_PATH: " + System.getProperty("BEAST_PACKAGE_PATH"));
        System.out.println("java.class.path entries: " + System.getProperty("java.class.path").split(":").length);

        assertFalse(dataTypes.isEmpty(), "DataType providers should not be empty");
        assertTrue(dataTypes.stream().anyMatch(s -> s.contains("Nucleotide")),
            "Should find Nucleotide data type");
    }
}
