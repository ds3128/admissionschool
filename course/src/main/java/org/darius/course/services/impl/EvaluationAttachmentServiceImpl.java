package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.CreateAttachmentRequest;
import org.darius.course.dtos.responses.EvaluationAttachmentResponse;
import org.darius.course.entities.*;
import org.darius.course.exceptions.*;
import org.darius.course.mapper.CourseMapper;
import org.darius.course.repositories.*;
import org.darius.course.services.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*; import java.util.Set;

@Service @RequiredArgsConstructor @Slf4j
public class EvaluationAttachmentServiceImpl implements EvaluationAttachmentService {

    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip","application/x-zip-compressed","image/png","image/jpeg");

    private final EvaluationAttachmentRepository attachmentRepository;
    private final EvaluationRepository           evaluationRepository;
    private final TeacherAssignmentService       assignmentService;
    private final CourseMapper                   mapper;

    @Override @Transactional
    public EvaluationAttachmentResponse addAttachment(Long evalId, String teacherId, CreateAttachmentRequest req, MultipartFile file) {
        Evaluation ev = evaluationRepository.findById(evalId).orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable"));
        if (!assignmentService.isTeacherAssigned(teacherId, ev.getMatiere().getId(), ev.getSemester().getId()))
            throw new ForbiddenException("Vous n'êtes pas affecté à cette matière");
        if (file == null || file.isEmpty()) throw new InvalidOperationException("Un fichier est requis");
        if (file.getSize() > MAX_BYTES) throw new InvalidOperationException("Fichier trop volumineux (max 20 Mo)");
        if (!ALLOWED.contains(file.getContentType())) throw new InvalidOperationException("Format non supporté : " + file.getContentType());
        String url = "/uploads/attachments/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        return mapper.toAttachmentResponse(attachmentRepository.save(EvaluationAttachment.builder()
                .evaluation(ev).title(req.getTitle()).description(req.getDescription()).fileUrl(url)
                .fileName(file.getOriginalFilename()).fileSize(file.getSize()).mimeType(file.getContentType())
                .isDeleted(false).uploadedBy(teacherId).build()));
    }

    @Override @Transactional
    public void delete(Long id, String teacherId) {
        EvaluationAttachment att = attachmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable"));
        if (!att.getUploadedBy().equals(teacherId)) throw new ForbiddenException("Vous n'êtes pas l'auteur");
        att.setDeleted(true); attachmentRepository.save(att);
    }

    @Override @Transactional(readOnly = true)
    public List<EvaluationAttachmentResponse> getByEvaluation(Long evalId) {
        return attachmentRepository.findByEvaluation_IdAndIsDeletedFalse(evalId).stream().map(mapper::toAttachmentResponse).toList();
    }
}
