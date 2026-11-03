package contactmanagement.controller;

import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;
import contactmanagement.model.WebResponse;
import contactmanagement.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing contact endpoints under {@code /api/contacts}.
 *
 * <p>The controller only handles HTTP concerns (mapping, validation trigger, response envelope);
 * all business rules live in {@link ContactService}.
 */
@RestController
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * Creates a contact for the currently authenticated user.
     *
     * <p>Requires the {@code X-API-TOKEN} header. {@code User} is the first parameter, so
     * {@link contactmanagement.resolver.UserArgumentResolver} authenticates the request before
     * the body is validated: a missing or invalid token returns 401 even when the body is also
     * invalid.
     *
     * @param user    the authenticated user, injected by the argument resolver (not read from the
     *                request body)
     * @param request the contact data; validated before the method is invoked
     * @return a {@link WebResponse} with HTTP 200 containing the created contact
     */
    @PostMapping(
            path = "/api/contacts",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<ContactResponse> create(User user, @Valid @RequestBody CreateContactRequest request){
        ContactResponse response = contactService.create(user, request);
        return WebResponse.<ContactResponse>builder().data(response).build();
    }

    @GetMapping(
            path = "/api/contacts/{contactId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<ContactResponse> get(User user, @PathVariable("contactId") String contactId){
        ContactResponse response = contactService.get(user, contactId);
        return WebResponse.<ContactResponse>builder().data(response).build();
    }
}