package com.ijspast.journal.service;

import com.ijspast.journal.entity.Author;
import com.ijspast.journal.entity.User;
import com.ijspast.journal.entity.UserRole;
import com.ijspast.journal.exception.BadRequestException;
import com.ijspast.journal.exception.DuplicateResourceException;
import com.ijspast.journal.repository.AuthorRepository;
import com.ijspast.journal.repository.UserRepository;
import com.ijspast.journal.util.PhoneNumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserUniquenessValidationService {

    private static final Logger log = LoggerFactory.getLogger(UserUniquenessValidationService.class);

    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;

    public UserUniquenessValidationService(UserRepository userRepository,
                                           AuthorRepository authorRepository) {
        this.userRepository = userRepository;
        this.authorRepository = authorRepository;
    }

    /**
     * Sanitizes and trims an email address, converting to lower-case.
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Normalizes a phone number to standard international E.164-compatible form.
     */
    public String sanitizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String trimmed = phone.trim();
        if (!PhoneNumberUtils.isValidPhoneNumber(trimmed)) {
            throw new BadRequestException("The phone number provided ('" + trimmed + "') is invalid. Please enter a valid phone number (7-15 digits).");
        }
        return PhoneNumberUtils.normalize(trimmed);
    }

    /**
     * Validates uniqueness when creating a new Author or submitting a manuscript.
     * Enforces cross-role uniqueness against Editors and Reviewers, case-insensitivity,
     * and normalized phone uniqueness.
     */
    @Transactional(readOnly = true)
    public void validateNewAuthor(String email, String rawPhone) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Author email is required.");
        }
        String cleanEmail = sanitizeEmail(email);

        // 1. Email check across all users
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        User existingAuthorUser = null;

        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (existing.getRole() == UserRole.ROLE_EDITOR) {
                throw new DuplicateResourceException(
                        "An editor account already exists with this email address ('" + email.trim() + "'). Authors and editors cannot share the same email address."
                );
            } else if (existing.getRole() == UserRole.ROLE_REVIEWER) {
                throw new DuplicateResourceException(
                        "A reviewer account already exists with this email address ('" + email.trim() + "'). Authors and reviewers cannot share the same email address."
                );
            } else if (existing.getRole() == UserRole.ROLE_AUTHOR) {
                // Existing author resubmitting or submitting another paper
                existingAuthorUser = existing;
            }
        }

        // 2. Phone check across all users and authors
        if (rawPhone != null && !rawPhone.isBlank()) {
            String normalizedPhone = sanitizePhone(rawPhone);
            String excludeUserId = existingAuthorUser != null ? existingAuthorUser.getId() : null;

            Optional<User> conflictingUser = findConflictingUserByPhone(normalizedPhone, excludeUserId);
            if (conflictingUser.isPresent()) {
                User conflict = conflictingUser.get();
                if (conflict.getRole() == UserRole.ROLE_EDITOR) {
                    throw new DuplicateResourceException(
                            "An editor account already exists with this phone number ('" + rawPhone.trim() + "'). Authors and editors cannot share the same phone number."
                    );
                } else if (conflict.getRole() == UserRole.ROLE_AUTHOR) {
                    throw new DuplicateResourceException(
                            "Another author account is already registered with this phone number ('" + rawPhone.trim() + "'). Every author must have a unique phone number."
                    );
                } else {
                    throw new DuplicateResourceException(
                            "This phone number ('" + rawPhone.trim() + "') is already associated with another account in the portal."
                    );
                }
            }

            // Also check authors table directly if any detached author record exists
            Optional<Author> conflictingAuthor = findConflictingAuthorByPhone(normalizedPhone, excludeUserId);
            if (conflictingAuthor.isPresent()) {
                throw new DuplicateResourceException(
                        "Another author record is already registered with this phone number ('" + rawPhone.trim() + "'). Every author must have a unique phone number."
                );
            }
        }
    }

    /**
     * Validates uniqueness when creating a new Editor.
     * Enforces cross-role uniqueness against Authors and Reviewers.
     */
    @Transactional(readOnly = true)
    public void validateNewEditor(String email, String rawPhone) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Editor email is required.");
        }
        String cleanEmail = sanitizeEmail(email);

        // 1. Email check across all users
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (existingUserOpt.isPresent()) {
            User existing = existingUserOpt.get();
            if (existing.getRole() == UserRole.ROLE_AUTHOR) {
                throw new DuplicateResourceException(
                        "An author account already exists with this email address ('" + email.trim() + "'). Authors and editors cannot share the same email address."
                );
            } else if (existing.getRole() == UserRole.ROLE_EDITOR) {
                throw new DuplicateResourceException(
                        "An editor account already exists with this email address ('" + email.trim() + "'). Every editor must have a unique email address."
                );
            } else {
                throw new DuplicateResourceException(
                        "An account with email '" + email.trim() + "' already exists in the system."
                );
            }
        }

        // Check authors table explicitly in case of standalone author record
        if (authorRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new DuplicateResourceException(
                    "An author record already exists with this email address ('" + email.trim() + "'). Authors and editors cannot share the same email address."
            );
        }

        // 2. Phone check
        if (rawPhone != null && !rawPhone.isBlank()) {
            String normalizedPhone = sanitizePhone(rawPhone);
            Optional<User> conflictingUser = findConflictingUserByPhone(normalizedPhone, null);
            if (conflictingUser.isPresent()) {
                User conflict = conflictingUser.get();
                if (conflict.getRole() == UserRole.ROLE_AUTHOR) {
                    throw new DuplicateResourceException(
                            "An author account already exists with this phone number ('" + rawPhone.trim() + "'). Authors and editors cannot share the same phone number."
                    );
                } else if (conflict.getRole() == UserRole.ROLE_EDITOR) {
                    throw new DuplicateResourceException(
                            "Another editor account is already registered with this phone number ('" + rawPhone.trim() + "'). Every editor must have a unique phone number."
                    );
                } else {
                    throw new DuplicateResourceException(
                            "This phone number ('" + rawPhone.trim() + "') is already associated with another account in the portal."
                    );
                }
            }

            Optional<Author> conflictingAuthor = findConflictingAuthorByPhone(normalizedPhone, null);
            if (conflictingAuthor.isPresent()) {
                throw new DuplicateResourceException(
                        "An author record already exists with this phone number ('" + rawPhone.trim() + "'). Authors and editors cannot share the same phone number."
                );
            }
        }
    }

    /**
     * Validates uniqueness when updating an existing user (Author, Editor, Reviewer).
     */
    @Transactional(readOnly = true)
    public void validateUpdateUser(String currentUserId, String newEmail, String rawPhone) {
        // 1. Email check
        if (newEmail != null && !newEmail.isBlank()) {
            String cleanEmail = sanitizeEmail(newEmail);
            Optional<User> userWithEmail = userRepository.findByEmailIgnoreCase(cleanEmail);
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(currentUserId)) {
                User conflict = userWithEmail.get();
                User currentUser = userRepository.findById(currentUserId).orElse(null);
                UserRole currentRole = currentUser != null ? currentUser.getRole() : null;

                if (currentRole == UserRole.ROLE_AUTHOR && conflict.getRole() == UserRole.ROLE_EDITOR) {
                    throw new DuplicateResourceException(
                            "Cannot update email: An editor account already exists with email '" + newEmail.trim() + "'. Authors and editors cannot share the same email address."
                    );
                } else if (currentRole == UserRole.ROLE_EDITOR && conflict.getRole() == UserRole.ROLE_AUTHOR) {
                    throw new DuplicateResourceException(
                            "Cannot update email: An author account already exists with email '" + newEmail.trim() + "'. Authors and editors cannot share the same email address."
                    );
                } else {
                    throw new DuplicateResourceException(
                            "An account with email '" + newEmail.trim() + "' already exists in the system."
                    );
                }
            }
        }

        // 2. Phone check
        if (rawPhone != null && !rawPhone.isBlank()) {
            String normalizedPhone = sanitizePhone(rawPhone);
            Optional<User> conflictingUser = findConflictingUserByPhone(normalizedPhone, currentUserId);
            if (conflictingUser.isPresent()) {
                User conflict = conflictingUser.get();
                User currentUser = userRepository.findById(currentUserId).orElse(null);
                UserRole currentRole = currentUser != null ? currentUser.getRole() : null;

                if (currentRole == UserRole.ROLE_AUTHOR && conflict.getRole() == UserRole.ROLE_EDITOR) {
                    throw new DuplicateResourceException(
                            "Cannot update phone: An editor account already exists with phone '" + rawPhone.trim() + "'. Authors and editors cannot share the same phone number."
                    );
                } else if (currentRole == UserRole.ROLE_EDITOR && conflict.getRole() == UserRole.ROLE_AUTHOR) {
                    throw new DuplicateResourceException(
                            "Cannot update phone: An author account already exists with phone '" + rawPhone.trim() + "'. Authors and editors cannot share the same phone number."
                    );
                } else {
                    throw new DuplicateResourceException(
                            "This phone number ('" + rawPhone.trim() + "') is already registered to another user."
                    );
                }
            }

            Optional<Author> conflictingAuthor = findConflictingAuthorByPhone(normalizedPhone, currentUserId);
            if (conflictingAuthor.isPresent()) {
                throw new DuplicateResourceException(
                        "This phone number ('" + rawPhone.trim() + "') is already associated with an author record."
                );
            }
        }
    }

    /**
     * Checks if any user in the system has a phone number matching rawPhone
     * via normalized E.164 equality or matching national significant digits.
     */
    public Optional<User> findConflictingUserByPhone(String rawPhone, String excludeUserId) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return Optional.empty();
        }

        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            if (excludeUserId != null && excludeUserId.equals(u.getId())) {
                continue;
            }
            if (u.getPhone() != null && !u.getPhone().isBlank()) {
                if (PhoneNumberUtils.isDuplicate(u.getPhone(), rawPhone)) {
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if any author record has a phone number matching rawPhone.
     */
    public Optional<Author> findConflictingAuthorByPhone(String rawPhone, String excludeUserId) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return Optional.empty();
        }

        List<Author> allAuthors = authorRepository.findAll();
        for (Author a : allAuthors) {
            if (excludeUserId != null && a.getUser() != null && excludeUserId.equals(a.getUser().getId())) {
                continue;
            }
            if (a.getPhone() != null && !a.getPhone().isBlank()) {
                if (PhoneNumberUtils.isDuplicate(a.getPhone(), rawPhone)) {
                    return Optional.of(a);
                }
            }
        }
        return Optional.empty();
    }
}

