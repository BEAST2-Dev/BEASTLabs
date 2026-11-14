package test.beastlabs;

import beast.base.core.BEASTInterface;
import beast.base.evolution.datatype.DataType;
import beast.pkgmgmt.BEASTClassLoader;
import beastlabs.math.distributions.WeibullDistribution;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceDiscoveryTest {

    /** modules BEAST 2.8 provides to packages (BEAST.base and the launcher's boot layer) */
    static final Set<String> PROVIDED_BY_BEAST = Set.of("java.base", "java.xml", "java.scripting",
            "beast.base", "beast.pkgmgmt", "beagle",
            "org.apache.commons.statistics.distribution", "org.apache.commons.numbers.gamma");

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

    @Test
    public void testBeastLabsServiceDiscovery() throws Exception {
        Set<String> beastObjects = BEASTClassLoader.loadService(BEASTInterface.class);

        // BEAUti loads WeibullDistribution first, through fxtemplates/Weibull.xml (issue #32)
        String className = WeibullDistribution.class.getName();
        assertTrue(beastObjects.contains(className), className + " should be provided as BEASTInterface service");
        assertNotNull(BEASTClassLoader.forName(className), className + " should be loadable");
    }

    /**
     * Issue #32: BEAST only loads a package module if every non-static 'requires' is already
     * loaded by BEAST, otherwise it skips beast.labs and no BEASTLabs class can be found.
     * Jars shipped in the package lib (e.g. nashorn) must therefore be 'requires static'.
     */
    @Test
    public void testHardRequirementsAreProvidedByBeast() throws Exception {
        Path classes = Path.of(WeibullDistribution.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        ModuleDescriptor descriptor;
        try (InputStream in = Files.newInputStream(classes.resolve("module-info.class"))) {
            descriptor = ModuleDescriptor.read(in);
        }
        for (Requires r : descriptor.requires()) {
            if (!r.modifiers().contains(Requires.Modifier.STATIC) && !r.modifiers().contains(Requires.Modifier.MANDATED)) {
                assertTrue(PROVIDED_BY_BEAST.contains(r.name()),
                        "beast.labs hard-requires " + r.name() + ", which BEAST does not provide, " +
                        "so BEAST would skip the whole module (issue #32): use 'requires static' instead");
            }
        }
    }
}
