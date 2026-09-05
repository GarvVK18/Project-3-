package com.iam.server.service;

import com.iam.server.dto.MfaChallengeResponse;
import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.entity.User;

public interface MfaService {

    MfaSetupResponse setupMfa(String username, String mfaType);

    boolean enableMfa(String username, String code, String mfaType, String email, String phoneNumber);

    boolean disableMfa(String username);

    MfaChallengeResponse initiateLoginChallenge(User user);

    boolean sendOtpForChallenge(String tempToken);

    String verifyLoginChallenge(String tempToken, String code);
}
