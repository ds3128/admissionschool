package org.darius.course.services.impl;

import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.darius.course.dtos.requests.*;
import org.darius.course.dtos.responses.CourseResourceResponse;
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
public class CourseResourceServiceImpl implements CourseResourceService {

    private static final long MAX_BYTES = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip","application/x-zip-compressed","video/mp4","audio/mpeg","image/png","image/jpeg");

    private final CourseResourceRepository resourceRepository;
    private final MatiereRepository        matiereRepository;
    private final SemesterRepository       semesterRepository;
    private final EnrollmentRepository     enrollmentRepository;
    private final TeacherAssignmentService assignmentService;
    private final CourseMapper             mapper;

    @Override @Transactional
    public CourseResourceResponse addResource(Long matiereId, String teacherId, CreateCourseResourceRequest req, MultipartFile file) {
        Matiere  m   = matiereRepository.findById(matiereId).orElseThrow(() -> new ResourceNotFoundException("Matière introuvable"));
        Semester sem = semesterRepository.findById(req.getSemesterId()).orElseThrow(() -> new ResourceNotFoundException("Semestre introuvable"));
        if (!assignmentService.isTeacherAssigned(teacherId, matiereId, req.getSemesterId()))
            throw new ForbiddenException("Vous n'êtes pas affecté à cette matière");
        String url; String fileName = null; Long fileSize = null; String mime = null;
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > MAX_BYTES) throw new InvalidOperationException("Fichier trop volumineux (max 50 Mo)");
            if (!ALLOWED.contains(file.getContentType())) throw new InvalidOperationException("Format non supporté : " + file.getContentType());
            url = "/uploads/resources/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            fileName = file.getOriginalFilename(); fileSize = file.getSize(); mime = file.getContentType();
        } else if (req.getExternalUrl() != null && !req.getExternalUrl().isBlank()) {
            url = req.getExternalUrl();
        } else throw new InvalidOperationException("Un fichier ou un lien externe est requis");
        return mapper.toCourseResourceResponse(resourceRepository.save(CourseResource.builder()
                .matiere(m).semester(sem).title(req.getTitle()).description(req.getDescription())
                .type(req.getType()).fileUrl(url).fileName(fileName).fileSize(fileSize).mimeType(mime)
                .isPublished(false).isDeleted(false).uploadedBy(teacherId).build()));
    }

    @Override @Transactional
    public CourseResourceResponse update(Long id, String teacherId, UpdateCourseResourceRequest req) {
        CourseResource r = findOrThrow(id); checkOwner(r, teacherId);
        if (req.getTitle()       != null) r.setTitle(req.getTitle());
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getType()        != null) r.setType(req.getType());
        return mapper.toCourseResourceResponse(resourceRepository.save(r));
    }

    @Override @Transactional
    public CourseResourceResponse publish(Long id, String teacherId) {
        CourseResource r = findOrThrow(id); checkOwner(r, teacherId);
        if (r.isDeleted()) throw new InvalidOperationException("Ce support a été supprimé");
        r.setPublished(true); return mapper.toCourseResourceResponse(resourceRepository.save(r));
    }

    @Override @Transactional
    public CourseResourceResponse unpublish(Long id, String teacherId) {
        CourseResource r = findOrThrow(id); checkOwner(r, teacherId);
        r.setPublished(false); return mapper.toCourseResourceResponse(resourceRepository.save(r));
    }

    @Override @Transactional
    public void delete(Long id, String teacherId) {
        CourseResource r = findOrThrow(id); checkOwner(r, teacherId);
        r.setDeleted(true); r.setPublished(false); resourceRepository.save(r);
    }

    @Override @Transactional(readOnly = true)
    public CourseResourceResponse getById(Long id) { return mapper.toCourseResourceResponse(findOrThrow(id)); }

    @Override @Transactional(readOnly = true)
    public List<CourseResourceResponse> getPublishedByMatiere(Long matiereId, String studentId) {
        Semester cur = semesterRepository.findByIsCurrent(true).orElse(null);
        if (cur != null && !enrollmentRepository.existsByStudentIdAndMatiere_IdAndSemester_Id(studentId, matiereId, cur.getId()))
            throw new ForbiddenException("Vous n'êtes pas inscrit à cette matière");
        return resourceRepository.findPublishedByMatiereId(matiereId).stream().map(mapper::toCourseResourceResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<CourseResourceResponse> getMyResources(String teacherId) {
        return resourceRepository.findByTeacherId(teacherId).stream().map(mapper::toCourseResourceResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<CourseResourceResponse> getRecentPublished(Long matiereId, int limit) {
        return resourceRepository.findTop3PublishedByMatiereId(matiereId).stream().map(mapper::toCourseResourceResponse).toList();
    }

    private void checkOwner(CourseResource r, String teacherId) {
        if (!r.getUploadedBy().equals(teacherId)) throw new ForbiddenException("Vous n'êtes pas l'auteur de ce support");
    }
    private CourseResource findOrThrow(Long id) {
        return resourceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Support introuvable : id=" + id));
    }
}
