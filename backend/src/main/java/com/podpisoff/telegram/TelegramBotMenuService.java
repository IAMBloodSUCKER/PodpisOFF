package com.podpisoff.telegram;



import com.podpisoff.notification.TelegramNotificationService;

import com.podpisoff.user.LocaleCode;

import com.podpisoff.user.User;

import com.podpisoff.user.UserRepository;

import java.time.YearMonth;

import java.util.List;

import java.util.Map;

import java.util.Optional;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



@Service

public class TelegramBotMenuService {



    private static final Logger log = LoggerFactory.getLogger(TelegramBotMenuService.class);



    private final TelegramBotQueryService telegramBotQueryService;

    private final TelegramNotificationService telegramNotificationService;

    private final TelegramBotApiClient telegramBotApiClient;

    private final UserRepository userRepository;

    private final TelegramLinkService telegramLinkService;

    private final ConcurrentHashMap<String, Long> guestMenuMessageIds = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, TelegramMenuAction> lastMenuActionByChat = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, YearMonth> summaryMonthByChat = new ConcurrentHashMap<>();



    public TelegramBotMenuService(TelegramBotQueryService telegramBotQueryService,

                                  TelegramNotificationService telegramNotificationService,

                                  TelegramBotApiClient telegramBotApiClient,

                                  UserRepository userRepository,

                                  TelegramLinkService telegramLinkService) {

        this.telegramBotQueryService = telegramBotQueryService;

        this.telegramNotificationService = telegramNotificationService;

        this.telegramBotApiClient = telegramBotApiClient;

        this.userRepository = userRepository;

        this.telegramLinkService = telegramLinkService;

    }



    @Transactional

    public void openMenuForChat(String chatId) {

        Optional<User> linkedUser = telegramBotQueryService.findLinkedUser(chatId);

        if (linkedUser.isPresent()) {

            openLinkedMenu(linkedUser.get(), chatId, TelegramMenuAction.HOME);

            return;

        }

        showUnlinked(chatId);

    }



    @Transactional

    public void openLinkedMenu(User user, String chatId, TelegramMenuAction action) {

        renderMenu(user, chatId, action, null);

    }



    @Transactional

    public void handleMenuAction(String chatId, long messageId, TelegramMenuAction action, String callbackId) {

        Optional<User> linkedUser = telegramBotQueryService.findLinkedUser(chatId);

        if (linkedUser.isEmpty()) {

            telegramNotificationService.answerCallback(callbackId, null);

            showUnlinked(chatId);

            return;

        }



        User user = linkedUser.get();

        if (action == TelegramMenuAction.DISABLE) {

            renderDisconnected(chatId, messageId);

            telegramLinkService.handleStop(chatId);

            clearMenuMessageId(chatId, user);

            telegramNotificationService.answerCallback(

                callbackId,

                user.getLocale() == LocaleCode.RU ? "Отключено" : "Disabled"

            );

            return;

        }



        if (action == TelegramMenuAction.EXPORT) {

            if (!telegramBotQueryService.isPro(user)) {

                renderProUpsell(user, chatId, messageId, action, callbackId);

                return;

            }

            sendExport(user, chatId, callbackId);

            return;

        }



        if (action == TelegramMenuAction.ANALYTICS && !telegramBotQueryService.isPro(user)) {

            renderProUpsell(user, chatId, messageId, action, callbackId);

            return;

        }



        if (action == TelegramMenuAction.SUMMARY) {

            summaryMonthByChat.put(chatId, YearMonth.now());

        } else if (action == TelegramMenuAction.SUMMARY_PREV || action == TelegramMenuAction.SUMMARY_NEXT) {

            YearMonth current = summaryMonthByChat.getOrDefault(chatId, YearMonth.now());

            YearMonth target = action == TelegramMenuAction.SUMMARY_PREV

                ? current.minusMonths(1)

                : current.plusMonths(1);

            if (target.isAfter(YearMonth.now())) {

                telegramNotificationService.answerCallback(

                    callbackId,

                    user.getLocale() == LocaleCode.RU ? "Будущие месяцы недоступны" : "Future months unavailable"

                );

                return;

            }

            summaryMonthByChat.put(chatId, target);

            action = TelegramMenuAction.SUMMARY;

        }



        renderMenu(user, chatId, action, messageId);

        telegramNotificationService.answerCallback(callbackId, null);

    }



    @Transactional

    public void showInvalidLink(String chatId) {

        if (telegramBotQueryService.findLinkedUser(chatId).isPresent()) {

            openMenuForChat(chatId);

            return;

        }

        String text = TelegramBotMessages.invalidLink();

        replaceMenuMessage(chatId, null, text, TelegramBotKeyboards.unlinkedMenu(telegramBotQueryService.siteUrl()));

    }



    public void showUnlinkedPublic(String chatId) {

        showUnlinked(chatId);

    }



    public void deleteChatMessage(String chatId, long messageId) {

        tryDelete(chatId, messageId);

    }



    @Transactional

    public void refreshMenuToBottom(String chatId) {

        Optional<User> linkedUser = telegramBotQueryService.findLinkedUser(chatId);

        if (linkedUser.isEmpty()) {

            return;

        }



        User user = userRepository.findById(linkedUser.get().getId()).orElse(linkedUser.get());

        TelegramMenuAction action = lastMenuActionByChat.getOrDefault(chatId, TelegramMenuAction.HOME);
        if (!telegramBotQueryService.isPro(user)
            && (action == TelegramMenuAction.ANALYTICS || action == TelegramMenuAction.EXPORT)) {
            action = TelegramMenuAction.HOME;
        }



        Long oldMessageId = user.getTelegramMenuMessageId();

        if (oldMessageId != null) {

            tryDelete(chatId, oldMessageId);

        }



        String text = textForAction(chatId, user, action);

        List<List<Map<String, String>>> buttons = keyboardsFor(user, action);



        Long newMessageId = sendMenu(chatId, text, buttons);

        if (newMessageId != null) {

            saveMenuMessageId(chatId, user, newMessageId);

        }

    }



    private void showUnlinked(String chatId) {

        String text = TelegramBotMessages.unlinkedWelcome(null);

        replaceMenuMessage(chatId, null, text, TelegramBotKeyboards.unlinkedMenu(telegramBotQueryService.siteUrl()));

    }



    private void renderDisconnected(String chatId, long messageId) {

        String text = TelegramBotMessages.disconnected();

        try {

            telegramBotApiClient.editMessageText(

                chatId,

                messageId,

                text,

                keyboard(TelegramBotKeyboards.disconnectedMenu(telegramBotQueryService.siteUrl()))

            );

        } catch (Exception ex) {

            log.debug(

                "Failed to edit disconnected menu: {}",

                TelegramSecrets.safeErrorMessage(ex, telegramBotApiClient.tokenForLogging())

            );

            replaceMenuMessage(chatId, messageId, text, TelegramBotKeyboards.disconnectedMenu(telegramBotQueryService.siteUrl()));

        }

    }



    private void renderProUpsell(

        User user,

        String chatId,

        long messageId,

        TelegramMenuAction feature,

        String callbackId

    ) {

        lastMenuActionByChat.put(chatId, TelegramMenuAction.HOME);

        String text = TelegramBotReplyFormatter.proUpsell(user, feature, telegramBotQueryService.billingUrl());

        replaceMenuMessage(

            chatId,

            messageId,

            text,

            TelegramBotKeyboards.proUpsellMenu(user, telegramBotQueryService.billingUrl()),

            user

        );

        telegramNotificationService.answerCallback(callbackId, null);

    }



    private void sendExport(User user, String chatId, String callbackId) {

        boolean russian = user.getLocale() == LocaleCode.RU;

        try {

            byte[] data = telegramBotQueryService.exportExcel(user);

            telegramBotApiClient.sendDocument(chatId, data, "podpisoff-subscriptions.xlsx");

            telegramNotificationService.answerCallback(callbackId, russian ? "Файл отправлен" : "File sent");

        } catch (Exception ex) {

            log.warn(

                "Failed to export subscriptions to Telegram for {}: {}",

                chatId,

                TelegramSecrets.safeErrorMessage(ex, telegramBotApiClient.tokenForLogging())

            );

            telegramNotificationService.answerCallback(

                callbackId,

                russian ? "Не удалось отправить файл" : "Failed to send file"

            );

        }

    }



    private void renderMenu(User user, String chatId, TelegramMenuAction action, Long messageId) {

        lastMenuActionByChat.put(chatId, action);

        String text = textForAction(chatId, user, action);

        List<List<Map<String, String>>> buttons = keyboardsFor(user, action);

        Long targetMessageId = messageId != null ? messageId : resolveMenuMessageId(chatId, user);

        replaceMenuMessage(chatId, targetMessageId, text, buttons, user);

    }



    private String textForAction(String chatId, User user, TelegramMenuAction action) {

        boolean isPro = telegramBotQueryService.isPro(user);

        return switch (action) {

            case HOME -> TelegramBotReplyFormatter.home(user, telegramBotQueryService.siteUrl(), isPro);

            case SUBS -> TelegramBotReplyFormatter.subscriptions(

                user,

                telegramBotQueryService.subscriptions(user),

                isPro,

                telegramBotQueryService.includedSubscriptionCount(user)

            );

            case UPCOMING -> TelegramBotReplyFormatter.upcoming(

                user,

                telegramBotQueryService.summary(user).upcomingBilling()

            );

            case SUMMARY -> {

                YearMonth month = summaryMonthByChat.getOrDefault(chatId, YearMonth.now());

                yield TelegramBotReplyFormatter.summary(

                    user,

                    telegramBotQueryService.summary(user, month.getYear(), month.getMonthValue())

                );

            }

            case ANALYTICS -> TelegramBotReplyFormatter.analytics(user, telegramBotQueryService.analytics(user));

            case PLAN -> TelegramBotReplyFormatter.plan(

                user,

                isPro,

                telegramBotQueryService.subscriptions(user).size(),

                telegramBotQueryService.includedSubscriptionCount(user)

            );

            case DISABLE -> TelegramBotReplyFormatter.home(user, telegramBotQueryService.siteUrl(), isPro);

            default -> TelegramBotReplyFormatter.home(user, telegramBotQueryService.siteUrl(), isPro);

        };

    }



    private List<List<Map<String, String>>> keyboardsFor(User user, TelegramMenuAction action) {

        return TelegramBotKeyboards.forScreen(user, action, telegramBotQueryService.isPro(user));

    }



    private void replaceMenuMessage(String chatId, Long messageId, String text, List<List<Map<String, String>>> buttons) {

        replaceMenuMessage(chatId, messageId, text, buttons, null);

    }



    private void replaceMenuMessage(

        String chatId,

        Long messageId,

        String text,

        List<List<Map<String, String>>> buttons,

        User user

    ) {

        Long targetMessageId = messageId != null ? messageId : resolveMenuMessageId(chatId, user);

        if (targetMessageId != null) {

            try {

                telegramBotApiClient.editMessageText(chatId, targetMessageId, text, keyboard(buttons));

                saveMenuMessageId(chatId, user, targetMessageId);

                return;

            } catch (Exception ex) {

                log.debug(

                    "Failed to edit Telegram menu: {}",

                    TelegramSecrets.safeErrorMessage(ex, telegramBotApiClient.tokenForLogging())

                );

                tryDelete(chatId, targetMessageId);

            }

        }



        Long previousId = resolveMenuMessageId(chatId, user);

        if (previousId != null) {

            tryDelete(chatId, previousId);

        }



        Long newMessageId = sendMenu(chatId, text, buttons);

        if (newMessageId != null) {

            saveMenuMessageId(chatId, user, newMessageId);

        }

    }



    private Long resolveMenuMessageId(String chatId, User user) {

        if (user != null && user.getTelegramMenuMessageId() != null) {

            return user.getTelegramMenuMessageId();

        }

        return guestMenuMessageIds.get(chatId);

    }



    private void saveMenuMessageId(String chatId, User user, long messageId) {

        if (user != null) {

            user.setTelegramMenuMessageId(messageId);

            userRepository.save(user);

            guestMenuMessageIds.remove(chatId);

            return;

        }

        guestMenuMessageIds.put(chatId, messageId);

    }



    private void clearMenuMessageId(String chatId, User user) {

        if (user != null) {

            user.setTelegramMenuMessageId(null);

            userRepository.save(user);

        }

        guestMenuMessageIds.remove(chatId);

        lastMenuActionByChat.remove(chatId);

        summaryMonthByChat.remove(chatId);

    }



    private Long sendMenu(String chatId, String text, List<List<Map<String, String>>> buttons) {

        try {

            return telegramBotApiClient.sendMessageReturnId(chatId, text, keyboard(buttons));

        } catch (Exception ex) {

            log.warn(

                "Failed to send Telegram menu to {}: {}",

                chatId,

                TelegramSecrets.safeErrorMessage(ex, telegramBotApiClient.tokenForLogging())

            );

            return null;

        }

    }



    private void tryDelete(String chatId, long messageId) {

        try {

            telegramBotApiClient.deleteMessage(chatId, messageId);

        } catch (Exception ignored) {

            // Message may already be gone.

        }

    }



    private static Map<String, Object> keyboard(List<List<Map<String, String>>> inlineKeyboard) {

        return Map.of("inline_keyboard", inlineKeyboard);

    }

}

