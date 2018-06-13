/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.auth.service;

import java.util.function.Function;
import org.openlmis.auth.domain.ExpirationToken;
import org.openlmis.auth.domain.User;
import org.openlmis.auth.dto.referencedata.UserDto;
import org.openlmis.auth.i18n.ExposedMessageSource;
import org.openlmis.auth.repository.ExpirationTokenRepository;
import org.openlmis.auth.service.notification.NotificationService;
import org.openlmis.auth.service.referencedata.UserReferenceDataService;
import org.openlmis.util.NotificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;

public abstract class ExpirationTokenNotifier<T extends ExpirationToken> {
  public static final long TOKEN_VALIDITY_HOURS = 12;

  static final String MAIL_ADDRESS = System.getenv("MAIL_ADDRESS");

  @Autowired
  private UserReferenceDataService userReferenceDataService;

  @Autowired
  private NotificationService notificationService;

  @Autowired
  private ExposedMessageSource messageSource;

  T createExpirationToken(User user,
      ExpirationTokenRepository<T> repository, Function<User, T> creator) {
    T token = repository.findOneByUser(user);

    if (token != null) {
      repository.delete(token);
      // the JPA provider feels free to reorganize and/or optimize the database writes of the
      // pending changes from the persistent context, in particular the JPA provider does not
      // feel obliged to perform the database writes in the ordering and form implicated by
      // the individual changes of the persistent context.

      // the flush() flushes the changes to the database so when the flush() is executed after
      // delete(), sql gets executed and the following save will have no problems.
      repository.flush();
    }

    return repository.save(creator.apply(user));
  }

  void sendEmail(User user, String email, ExpirationToken token,
      String subjectKey, String bodyKey, String bodyUrl) {
    UserDto referenceDataUser = userReferenceDataService.findOne(user.getReferenceDataUserId());

    String[] bodyMsgArgs = {
        referenceDataUser.getFirstName(),
        referenceDataUser.getLastName(),
        bodyUrl + token.getId().toString()
    };
    String[] subjectMsgArgs = {};

    notificationService.send(new NotificationRequest(
        MAIL_ADDRESS,
        email,
        messageSource.getMessage(subjectKey, subjectMsgArgs, LocaleContextHolder.getLocale()),
        messageSource.getMessage(bodyKey, bodyMsgArgs, LocaleContextHolder.getLocale())));
  }

}
