/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.Context
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.MockBrowserDataHelper
import org.mozilla.fenix.helpers.RecyclerViewIdlingResource
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.longTapSelectItem
import org.mozilla.fenix.helpers.TestHelper.registerAndCleanupIdlingResources
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.historyMenu
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.multipleSelectionToolbar
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic functionality of history
 *
 */
class HistoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var mDevice: UiDevice

    @get:Rule
    val activityTestRule =
        AndroidComposeTestRule(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(),
        ) { it.activity }

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().targetContext.settings()
            .shouldShowJumpBackInCFR = false

        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // Clearing all history data after each test to avoid overlapping data
        val applicationContext: Context = activityTestRule.activity.applicationContext
        val historyStorage = PlacesHistoryStorage(applicationContext)

        runBlocking {
            historyStorage.deleteEverything()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/243285
    @Test
    fun verifyEmptyHistoryMenuTest() {
        homeScreen {
        }.openThreeDotMenu {
            verifyHistoryButton()
        }.openHistory {
            verifyHistoryMenuView()
            verifyEmptyHistoryView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2302742
    // Test running on beta/release builds in CI:
    // caution when making changes to it, so they don't block the builds
    @SmokeTest
    @Test
    fun verifyHistoryMenuWithHistoryItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                verifyHistoryMenuView()
                verifyVisitedTimeTitle()
                verifyFirstTestPageTitle("Test_Page_1")
                verifyTestPageUrl(firstWebPage.url)
                verifyDeleteHistoryItemButton("Test_Page_1")
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/243288
    @Test
    fun deleteHistoryItemTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                clickDeleteHistoryButton(firstWebPage.url.toString())
            }
            verifyUndoDeleteSnackBarButton()
            clickUndoDeleteButton()
            verifyHistoryItemExists(true, firstWebPage.url.toString())
            clickDeleteHistoryButton(firstWebPage.url.toString())
            verifyDeleteSnackbarText("Deleted")
            verifyEmptyHistoryView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1848881
    @SmokeTest
    @Test
    fun deleteAllHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                clickDeleteAllHistoryButton()
            }
            verifyDeleteConfirmationMessage()
            selectEverythingOption()
            cancelDeleteHistory()
            verifyHistoryItemExists(true, firstWebPage.url.toString())
            clickDeleteAllHistoryButton()
            verifyDeleteConfirmationMessage()
            selectEverythingOption()
            confirmDeleteAllHistory()
            verifyDeleteSnackbarText("Browsing data deleted")
            verifyEmptyHistoryView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/339690
    @Test
    fun historyMultiSelectionToolbarItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
            }
        }

        multipleSelectionToolbar {
            verifyMultiSelectionCheckmark()
            verifyMultiSelectionCounter()
            verifyShareHistoryButton()
            verifyCloseToolbarButton()
        }.closeToolbarReturnToHistory {
            verifyHistoryMenuView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/339696
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1807268")
    @Test
    fun openMultipleSelectedHistoryItemsInANewTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
            closeTab()
        }

        homeScreen { }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
                openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
            }
        }

        multipleSelectionToolbar {
        }.clickOpenNewTab {
            verifyExistingTabList()
            verifyNormalModeSelected()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/346098
    @Test
    fun openMultipleSelectedHistoryItemsInPrivateTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
                openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
            }
        }

        multipleSelectionToolbar {
        }.clickOpenPrivateTab {
            verifyPrivateModeSelected()
            verifyExistingTabList()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/346099
    @Test
    fun deleteMultipleSelectedHistoryItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            mDevice.waitForIdle()
            verifyUrl(secondWebPage.url.toString())
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 2),
            ) {
                verifyHistoryItemExists(true, firstWebPage.url.toString())
                verifyHistoryItemExists(true, secondWebPage.url.toString())
                longTapSelectItem(firstWebPage.url)
                longTapSelectItem(secondWebPage.url)
                openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
            }
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        historyMenu {
            verifyEmptyHistoryView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/339701
    @Test
    fun shareMultipleSelectedHistoryItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.history_list), 1),
            ) {
                longTapSelectItem(firstWebPage.url)
            }
        }

        multipleSelectionToolbar {
            clickShareHistoryButton()
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1715627
    @Test
    fun verifySearchHistoryViewTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openHistory {
        }.clickSearchButton {
            verifySearchView()
            verifySearchToolbar(true)
            verifySearchSelectorButton()
            verifySearchEngineIcon("history")
            verifySearchBarPlaceholder("Search history")
            verifySearchBarPosition(true)
            tapOutsideToDismissSearchBar()
            verifySearchToolbar(false)
            exitMenu()
        }
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openCustomizeSubMenu {
            clickTopToolbarToggle()
        }

        exitMenu()

        browserScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }.clickSearchButton {
            verifySearchView()
            verifySearchToolbar(true)
            verifySearchBarPosition(false)
            pressBack()
        }
        historyMenu {
            verifyHistoryMenuView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1715631
    @Test
    fun verifyVoiceSearchInHistoryTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }.clickSearchButton {
            verifySearchToolbar(true)
            verifySearchEngineIcon("history")
            startVoiceSearch()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1715632
    @Test
    fun verifySearchForHistoryItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getHTMLControlsFormAsset(mockWebServer)

        MockBrowserDataHelper.createHistoryItem(firstWebPage.url.toString())
        MockBrowserDataHelper.createHistoryItem(secondWebPage.url.toString())

        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }.clickSearchButton {
            // Search for a valid term
            typeSearch("generic")
            verifySearchEngineSuggestionResults(activityTestRule, firstWebPage.url.toString(), searchTerm = "generic")
            verifySuggestionsAreNotDisplayed(activityTestRule, secondWebPage.url.toString())
        }.dismissSearchBar {}
        historyMenu {
        }.clickSearchButton {
            // Search for invalid term
            typeSearch("Android")
            verifySuggestionsAreNotDisplayed(
                activityTestRule,
                firstWebPage.url.toString(),
                secondWebPage.url.toString(),
            )
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1715634
    @Test
    fun verifyDeletedHistoryItemsCanNotBeSearchedTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)
        val thirdWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent(firstWebPage.content)
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            verifyPageContent(secondWebPage.content)
        }
        navigationToolbar {
        }.enterURLAndEnterToBrowser(thirdWebPage.url) {
            verifyPageContent(thirdWebPage.content)
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryListExists()
            clickDeleteHistoryButton(firstWebPage.title)
            verifyHistoryItemExists(false, firstWebPage.title)
            clickDeleteHistoryButton(secondWebPage.title)
            verifyHistoryItemExists(false, secondWebPage.title)
        }.clickSearchButton {
            // Search for a valid term
            typeSearch("generic")
            verifySuggestionsAreNotDisplayed(activityTestRule, firstWebPage.url.toString())
            verifySuggestionsAreNotDisplayed(activityTestRule, secondWebPage.url.toString())
            verifySearchEngineSuggestionResults(
                activityTestRule,
                thirdWebPage.url.toString(),
                searchTerm = "generic",
            )
            pressBack()
        }
        historyMenu {
            clickDeleteHistoryButton(thirdWebPage.title)
            verifyHistoryItemExists(false, firstWebPage.title)
        }.clickSearchButton {
            // Search for a valid term
            typeSearch("generic")
            verifySuggestionsAreNotDisplayed(activityTestRule, thirdWebPage.url.toString())
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/903590
    // Test running on beta/release builds in CI:
    // caution when making changes to it, so they don't block the builds
    @SmokeTest
    @Test
    fun noHistoryInPrivateBrowsingTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.togglePrivateBrowsingMode()

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openHistory {
            verifyEmptyHistoryView()
        }
    }
}
