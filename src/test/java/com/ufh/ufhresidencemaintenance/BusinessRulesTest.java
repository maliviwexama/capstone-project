package com.ufh.ufhresidencemaintenance;

import com.ufh.ufhresidencemaintenance.model.Complaint;
import com.ufh.ufhresidencemaintenance.model.ComplaintCategory;
import com.ufh.ufhresidencemaintenance.model.ComplaintStatus;
import com.ufh.ufhresidencemaintenance.util.LoginSecurityPolicy;
import com.ufh.ufhresidencemaintenance.util.PdfUploadValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BusinessRulesTest {
    @Test
    void validPdfProofIsAccepted() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("proofDocument", "residence.pdf", "application/pdf", "%PDF-1.7\nvalid test document".getBytes(StandardCharsets.US_ASCII));
        assertTrue(PdfUploadValidator.isValid(pdf));
    }

    @Test
    void nonPdfProofIsRejected() throws Exception {
        MockMultipartFile image = new MockMultipartFile("proofDocument", "residence.jpg", "image/jpeg", new byte[]{1,2,3,4,5});
        assertFalse(PdfUploadValidator.isValid(image));
    }

    @Test
    void fifthFailedLoginTriggersLock() {
        assertFalse(LoginSecurityPolicy.shouldLock(4));
        assertTrue(LoginSecurityPolicy.shouldLock(5));
    }

    @Test
    void rejectedComplaintCanBeArchivedWithoutBecomingResolved() {
        Complaint complaint = new Complaint();
        complaint.setStatus(ComplaintStatus.REJECTED);
        complaint.setArchived(true);
        assertEquals(ComplaintStatus.REJECTED, complaint.getStatus());
        assertEquals("Archived", complaint.getDisplayStatus());
    }

    @Test
    void electricalComplaintHasHighestBaseSeverity() {
        assertEquals(5, ComplaintCategory.ELECTRICAL.getSeverityWeight());
        assertEquals(1, ComplaintCategory.FURNITURE.getSeverityWeight());
    }
}
