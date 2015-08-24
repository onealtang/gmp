// Copyright 2013 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package adwords.axis.auth;

import com.google.api.ads.adwords.axis.factory.AdWordsServices;
import com.google.api.ads.adwords.axis.v201502.cm.Campaign;
import com.google.api.ads.adwords.axis.v201502.cm.CampaignPage;
import com.google.api.ads.adwords.axis.v201502.cm.CampaignServiceInterface;
import com.google.api.ads.adwords.axis.v201502.cm.Selector;
import com.google.api.ads.adwords.lib.client.AdWordsSession;
import com.google.api.ads.adwords.lib.jaxb.v201502.DownloadFormat;
import com.google.api.ads.adwords.lib.jaxb.v201502.ReportDefinition;
import com.google.api.ads.adwords.lib.jaxb.v201502.ReportDefinitionDateRangeType;
import com.google.api.ads.adwords.lib.jaxb.v201502.ReportDefinitionReportType;
import com.google.api.ads.adwords.lib.utils.ReportDownloadResponse;
import com.google.api.ads.adwords.lib.utils.v201502.ReportDownloader;
import com.google.api.ads.common.lib.conf.ConfigurationLoadException;
import com.google.api.ads.common.lib.exception.ValidationException;
import com.google.api.ads.common.lib.utils.Streams;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * This example demonstrates how to create a Credential object from scratch.<br>
 * This example is *not* meant to be used with our other examples, but shows
 * how you might use the general OAuth2 libraries to add OAuth2 to your
 * existing application.<br>
 * <br>
 * For an alternative to service accounts, installed applications, or a web
 * application that will not need to have multiple users log in, using
 * OfflineCredentials to generate a refreshable OAuth2
 * credential instead will be much easier.
 *
 * @author Adam Rogal
 */
public class AdvancedCreateCredentialFromScratch {

  private static final String SCOPE = "https://www.googleapis.com/auth/adwords";

  // This callback URL will allow you to copy the token from the success screen.
  // This must match the one associated with your client ID.
  private static final String CALLBACK_URL = "urn:ietf:wg:oauth:2.0:oob";

  // If you do not have a client ID or secret, please create one in the
  // API console: https://console.developers.google.com/project
  private static final String CLIENT_ID = "INSERT_CLIENT_ID_HERE";
  private static final String CLIENT_SECRET = "INSERT_CLIENT_SECRET_HERE";

  // The current user that is authenticating. This is typically a primary key
  // you define yourself that you will reference later in your code when
  // you retrieve the credential for that user.
  private static final String USER_ID = "INSERT_USER_ID_HERE";

  private static void authorize(DataStoreFactory storeFactory, String userId) throws Exception {
    // Depending on your application, there may be more appropriate ways of
    // performing the authorization flow (such as on a servlet), see
    // https://developers.google.com/api-client-library/java/google-api-java-client/oauth2#authorization_code_flow
    // for more information.
    GoogleAuthorizationCodeFlow authorizationFlow = new GoogleAuthorizationCodeFlow.Builder(
        new NetHttpTransport(),
        new JacksonFactory(),
        CLIENT_ID,
        CLIENT_SECRET,
        Lists.newArrayList(SCOPE))
        .setDataStoreFactory(storeFactory)
        // Set the access type to offline so that the token can be refreshed.
        // By default, the library will automatically refresh tokens when it
        // can, but this can be turned off by setting
        // api.adwords.refreshOAuth2Token=false in your ads.properties file.
        .setAccessType("offline").build();

    String authorizeUrl =
        authorizationFlow.newAuthorizationUrl().setRedirectUri(CALLBACK_URL).build();
    System.out.println("Paste this url in your browser: \n" + authorizeUrl + '\n');

    // Wait for the authorization code.
    System.out.println("Type the code you received here: ");
    String authorizationCode = new BufferedReader(new InputStreamReader(System.in)).readLine();

    // Authorize the OAuth2 token.
    GoogleAuthorizationCodeTokenRequest tokenRequest =
        authorizationFlow.newTokenRequest(authorizationCode);
    tokenRequest.setRedirectUri(CALLBACK_URL);
    GoogleTokenResponse tokenResponse = tokenRequest.execute();

    // Store the credential for the user.
    authorizationFlow.createAndStoreCredential(tokenResponse, userId);
  }

  private static AdWordsSession createAdWordsSession(
      DataStoreFactory dataStoreFactory, String userId)
      throws IOException, ValidationException, ConfigurationLoadException {
    // Create a GoogleCredential with minimal information.
    GoogleAuthorizationCodeFlow authorizationFlow = new GoogleAuthorizationCodeFlow.Builder(
        new NetHttpTransport(),
        new JacksonFactory(),
        CLIENT_ID,
        CLIENT_SECRET,
        Lists.newArrayList(SCOPE))
        .setDataStoreFactory(dataStoreFactory).build();

    // Load the credential.
    Credential credential = authorizationFlow.loadCredential(userId);

    // Construct a AdWordsSession.
    return new AdWordsSession.Builder()
        .fromFile()
        .withOAuth2Credential(credential)
        .build();
  }

  public static void runExample(
      AdWordsServices adWordsServices, AdWordsSession session, String reportFile) throws Exception {
    // Get the CampaignService.
    CampaignServiceInterface campaignService =
        adWordsServices.get(session, CampaignServiceInterface.class);

    // Create selector.
    Selector selector = new Selector();
    selector.setFields(new String[] {"Id", "Name"});

    // Get all campaigns.
    CampaignPage page = campaignService.get(selector);

    // Display campaigns.
    if (page.getEntries() != null) {
      for (Campaign campaign : page.getEntries()) {
        System.out.println("Campaign with name \"" + campaign.getName() + "\" and id \""
            + campaign.getId() + "\" was found.");
      }
    } else {
      System.out.println("No campaigns were found.");
    }

    // Create selector.
    com.google.api.ads.adwords.lib.jaxb.v201502.Selector reportSelector =
        new com.google.api.ads.adwords.lib.jaxb.v201502.Selector();
    reportSelector.getFields().addAll(Lists.newArrayList(
        "CampaignId",
        "AdGroupId",
        "Id",
        "CriteriaType",
        "Criteria",
        "Impressions",
        "Clicks",
        "Cost"));

    // Create report definition.
    ReportDefinition reportDefinition = new ReportDefinition();
    reportDefinition.setReportName("Criteria performance report #" + System.currentTimeMillis());
    reportDefinition.setDateRangeType(ReportDefinitionDateRangeType.YESTERDAY);
    reportDefinition.setReportType(ReportDefinitionReportType.CRITERIA_PERFORMANCE_REPORT);
    reportDefinition.setDownloadFormat(DownloadFormat.CSV);
    // Enable to allow rows with zero impressions to show.
    reportDefinition.setIncludeZeroImpressions(true);
    reportDefinition.setSelector(reportSelector);

    ReportDownloadResponse response =
        new ReportDownloader(session).downloadReport(reportDefinition);
    if (response.getHttpStatus() == HttpURLConnection.HTTP_OK) {
      FileOutputStream fos = new FileOutputStream(new File(reportFile));
      Streams.copy(response.getInputStream(), fos);
      fos.close();
      System.out.println("Report successfully downloaded: " + reportFile);
    } else {
      System.out.println("Report was not downloaded. " + response.getHttpStatus() + ": "
          + response.getHttpResponseMessage());
    }
  }

  public static void main(String[] args) throws Exception {
    if (CLIENT_ID.equals("INSERT_CLIENT_ID_HERE")
        || CLIENT_SECRET.equals("INSERT_CLIENT_SECRET_HERE")) {
      throw new IllegalArgumentException("Please input your client IDs or secret. "
          + "See https://console.developers.google.com/project");
    }

    // It is highly recommended that you use a credential store in your
    // application to store a per-user Credential.
    // See: https://developers.google.com/api-client-library/java/google-api-java-client/oauth2#data_store
    DataStoreFactory storeFactory = new MemoryDataStoreFactory();

    // Authorize and store your credential.
    authorize(storeFactory, USER_ID);

    // Create a AdWordsSession from the credential store. You will typically do this
    // in a servlet interceptor for a web application or per separate thread
    // of your offline application.
    AdWordsSession adWordsSession = createAdWordsSession(storeFactory, USER_ID);

    AdWordsServices adWordsServices = new AdWordsServices();

    // Location to download report to.
    String reportFile = System.getProperty("user.home") + File.separatorChar + "report.csv";

    runExample(adWordsServices, adWordsSession, reportFile);
  }
}
