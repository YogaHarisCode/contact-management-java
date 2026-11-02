package contactmanagement.service;

import contactmanagement.entity.User;
import contactmanagement.model.ContactResponse;
import contactmanagement.model.CreateContactRequest;

public interface ContactService {

    ContactResponse create(User user, CreateContactRequest request);
}
