package com.intive.shopme.registration;

import com.intive.shopme.common.ConvertibleController;
import com.intive.shopme.model.db.DbAddress;
import com.intive.shopme.model.db.DbRevokedToken;
import com.intive.shopme.model.db.DbInvoice;
import com.intive.shopme.model.db.DbUser;
import com.intive.shopme.model.db.DbVoivodeship;
import com.intive.shopme.model.rest.Address;
import com.intive.shopme.model.rest.Invoice;
import com.intive.shopme.model.rest.Role;
import com.intive.shopme.model.rest.Token;
import com.intive.shopme.model.rest.UserView;
import com.intive.shopme.model.rest.UserContext;
import com.intive.shopme.model.rest.UserCredentials;
import com.intive.shopme.model.rest.UserWrite;
import com.intive.shopme.model.rest.Voivodeship;
import com.intive.shopme.offer.OfferService;
import com.intive.shopme.voivodeship.VoivodeshipValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.intive.shopme.config.ApiUrl.USERS;
import static com.intive.shopme.config.ApiUrl.USERS_CURRENT;
import static com.intive.shopme.config.ApiUrl.USERS_LOGIN;
import static com.intive.shopme.config.ApiUrl.USERS_LOGOUT;
import static com.intive.shopme.config.AppConfig.CONSTRAINTS_JSON_KEY;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.BAD_USER_PASS;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.CREATED;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.DELETED;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.FORBIDDEN;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.LOGOUT;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.NOT_FOUND;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.REVOKED;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.SUCCESS;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.TOKEN_GENERATED;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.UNAUTHORIZED;
import static com.intive.shopme.config.SwaggerApiInfoConfigurer.Operations.VALIDATION_ERROR;

@RestController
@RequestMapping(value = USERS)
@Api(value = "user", description = "REST API for users operations", tags = "Users")
class UserController extends ConvertibleController<DbUser, UserView, UserWrite> {

    private final UserService service;
    private final OfferService offerService;
    private final ValidInvoiceIfInvoiceRequestedValidator invoiceRequestedValidator;
    private final VoivodeshipValidator voivodeshipValidator;
    private final EmailValidator emailValidator;
    private final PhoneValidator phoneValidator;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokensService;
    private final RevokedTokenService revokedTokenService;

    UserController(UserService service, OfferService offerService,
                   ValidInvoiceIfInvoiceRequestedValidator invoiceRequestedValidator,
                   VoivodeshipValidator voivodeshipValidator, EmailValidator emailValidator, PhoneValidator phoneValidator,
                   PasswordEncoder passwordEncoder, TokenService tokensService, RevokedTokenService revokedTokenService) {
        super(DbUser.class, UserView.class, UserWrite.class);
        this.service = service;
        this.offerService = offerService;
        this.invoiceRequestedValidator = invoiceRequestedValidator;
        this.voivodeshipValidator = voivodeshipValidator;
        this.emailValidator = emailValidator;
        this.phoneValidator = phoneValidator;
        this.passwordEncoder = passwordEncoder;
        this.tokensService = tokensService;
        this.revokedTokenService = revokedTokenService;
    }

    @PostMapping
    @ResponseStatus(value = HttpStatus.CREATED)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = CREATED),
            @ApiResponse(code = 422, message = VALIDATION_ERROR)
    })
    @ApiOperation(value = "Saves new user", response = UserView.class)
    ResponseEntity<?> add(@ApiParam(value = "New user's properties", required = true) @Valid @RequestBody UserWrite user,
                          Errors errors) {
        invoiceRequestedValidator.validate(user, errors);
        voivodeshipValidator.validate(user.getVoivodeship().getName(), errors);
        emailValidator.validate(user.getEmail(), errors);
        phoneValidator.validate(user.getPhoneNumber(), errors);
        if (errors.hasErrors()) {
            return new ResponseEntity<>(Map.of(CONSTRAINTS_JSON_KEY, createErrorString(errors)), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        final var dbUser = convertToDbModel(user);
        dbUser.setId(UUID.randomUUID());
        dbUser.addRole(Role.USER);
        if (service.count() == 0) {
            dbUser.addRole(Role.ADMIN);
        }
        return new ResponseEntity<>(convertToView(service.createOrUpdate(dbUser)), HttpStatus.CREATED);
    }

    @GetMapping(value = "{id}")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SUCCESS),
            @ApiResponse(code = 404, message = NOT_FOUND)
    })
    @ApiOperation(value = "Returns user by id")
    UserView get(@ApiParam(value = "ID number of user to be retrieved", required = true) @PathVariable UUID id) {
        return convertToView(service.get(id));
    }

    @DeleteMapping(value = "{id}")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = DELETED),
            @ApiResponse(code = 401, message = UNAUTHORIZED),
            @ApiResponse(code = 403, message = FORBIDDEN),
            @ApiResponse(code = 404, message = NOT_FOUND)
    })
    @ApiOperation(value = "Removes user by id", response = void.class)
    @PreAuthorize("hasAnyAuthority('USER')")
    ResponseEntity<?> delete(@ApiParam(value = "User id to be deleted (for confirmation)", required = true)
                             @PathVariable UUID id, @ApiIgnore @AuthenticationPrincipal UserContext userContext) {
        final var authenticatedUserId = userContext.getUserId();
        if (!id.equals(authenticatedUserId)) {
            return new ResponseEntity<>(Map.of("message", FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        final var authenticatedUser = service.get(authenticatedUserId);
        offerService.deleteAllByUser(authenticatedUser);

        service.delete(id);
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping(value = "email={email}")
    @ApiResponse(code = 200, message = SUCCESS)
    @ApiOperation(value = "Check if user with specified email exists in database")
    boolean existsByEmail(@ApiParam(value = "Email value to be checked is it free or not")
                          @PathVariable String email) {
        return service.existsByEmail(email);
    }

    @PostMapping(value = USERS_LOGIN)
    @ApiOperation("Log in to api")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = TOKEN_GENERATED),
            @ApiResponse(code = 400, message = BAD_USER_PASS)
    })
    @ResponseStatus(HttpStatus.OK)
    Token login(@ApiParam(value = "User's login credentials", required = true)
                @Valid @RequestBody UserCredentials credentials) {

        final var user = service.findOneByEmail(credentials.getEmail().toLowerCase());
        final String token = tokensService.exchangePasswordForToken(user, credentials.getPassword());

        return Token.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .surname(user.getSurname())
                .roles(user.getRoles())
                .expirationDate(tokensService.getExpirationDate())
                .jwt(token)
                .build();
    }

    @PostMapping(value = USERS_LOGOUT)
    @ApiOperation("Log out from api")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = LOGOUT),
            @ApiResponse(code = 401, message = REVOKED)
    })
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyAuthority('USER')")
    public void logout(@ApiIgnore @AuthenticationPrincipal UserContext userContext) {
        final var expirationDate = userContext.getExpirationDate();
        final var userId = userContext.getUserId();
        final var dbRevokedToken = new DbRevokedToken(userId, expirationDate);
        revokedTokenService.logout(dbRevokedToken);
    }

    @GetMapping(value = USERS_CURRENT)
    @ApiOperation(value = "Show current user")
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = SUCCESS),
            @ApiResponse(code = 401, message = UNAUTHORIZED)
    })
    @PreAuthorize("hasAnyAuthority('USER')")
    public UserContext whoAmI(@ApiIgnore @AuthenticationPrincipal UserContext userContext) {
        return userContext;
    }

    private static String createErrorString(Errors errors) {
        return errors.getAllErrors().stream()
                .map(ObjectError::toString)
                .collect(Collectors.joining(","));
    }

    @Override
    protected UserView convertToView(DbUser dbUser) {
        final var result = new UserView();

        result.setId(dbUser.getId());
        result.setName(dbUser.getName());
        result.setSurname(dbUser.getSurname());
        result.setEmail(dbUser.getEmail());
        result.setPhoneNumber(dbUser.getPhoneNumber());
        result.setBankAccount(dbUser.getBankAccount());

        if (dbUser.getAddress() != null) {
            result.setAddress(genericConvert(dbUser.getAddress(), Address.class));
        }

        if (dbUser.getVoivodeship() != null) {
            result.setVoivodeship(genericConvert(dbUser.getVoivodeship(), Voivodeship.class));
        }

        result.setInvoiceRequest(dbUser.getInvoiceRequest());

        if (dbUser.getInvoiceRequest() && dbUser.getInvoice() != null) {
            result.setInvoice(genericConvert(dbUser.getInvoice(), Invoice.class));
        }

        result.setAdditionalInfo(dbUser.getAdditionalInfo());

        return result;
    }

    @Override
    protected DbUser convertToDbModel(UserWrite user) {
        final var result = new DbUser();

        result.setName(user.getName());
        result.setSurname(user.getSurname());
        result.setEmail(user.getEmail().toLowerCase());
        result.setPassword(passwordEncoder.encode(user.getPassword()));
        result.setPhoneNumber(user.getPhoneNumber());
        result.setBankAccount(user.getBankAccount());
        result.setRoles(user.getRoles());

        if (user.getAddress() != null) {
            result.setAddress(genericConvert(user.getAddress(), DbAddress.class));
        }

        if (user.getVoivodeship() != null) {
            result.setVoivodeship(genericConvert(user.getVoivodeship(), DbVoivodeship.class));
        }

        result.setInvoiceRequest(user.getInvoiceRequest());

        if (user.getInvoiceRequest() && user.getInvoice() != null) {
            result.setInvoice(genericConvert(user.getInvoice(), DbInvoice.class));
        }

        result.setAdditionalInfo(user.getAdditionalInfo());

        return result;
    }
}
