package contactmanagement.service;

import contactmanagement.model.RegisterUserRequest;
import contactmanagement.model.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);
}
