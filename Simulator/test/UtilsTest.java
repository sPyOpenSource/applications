
import java.io.File;
import java.io.IOException;
import nl.lxtreme.binutils.elf.Elf;
import nl.lxtreme.binutils.elf.Header;
import org.junit.Test;

/**
 *
 * @author xuyi
 */
public class UtilsTest {
    @Test
    public void test() {
        File elfFile = new File("/Users/xuyi/Source/OS/bootboot/mykernel/mykernel.x86_64.elf");

        try {
            Elf elf = new Elf(elfFile);
            System.out.println(elf);
            /*for(ProgramHeader header:elf.programHeaders){
                System.out.println(header.virtualAddress);
            }*/
            // Verkrijg de ELF header informatie
            Header header = elf.header;
            System.out.println("Machine type: " + header.machineType);

            // Loop door alle secties (vergelijkbaar met 'readelf -S')
            /*for (SectionHeader section : elf.sectionHeaders) {
                System.out.printf("Sectie: %s | Adres: 0x%X | Grootte: %d bytes%n",
                        section.getName(), 
                        section.virtualAddress, 
                        section.size);
            }*/
        } catch (IOException e) {
            System.err.println("Fout bij het lezen van ELF: " + e.getMessage());
        }
    }
}
