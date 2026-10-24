package contactmanagement.service;

import contactmanagement.model.LoginResponse;
import contactmanagement.model.LoginUserRequest;


public interface AuthService {


    LoginResponse login(LoginUserRequest request);
}