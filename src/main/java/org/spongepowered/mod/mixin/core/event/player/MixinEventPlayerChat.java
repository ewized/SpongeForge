/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.mod.mixin.core.event.player;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ServerChatEvent;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.interfaces.IMixinInitCause;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.mod.mixin.core.fml.common.eventhandler.MixinEvent;

import java.util.Optional;

import javax.annotation.Nullable;

@NonnullByDefault
@Mixin(value = ServerChatEvent.class, remap = false)
public abstract class MixinEventPlayerChat extends MixinEvent implements MessageChannelEvent.Chat, IMixinInitCause {

    private ChatComponentTranslation forgeComponent;

    private final MessageFormatter formatter = new MessageFormatter();
    private Text originalSpongeMessage;
    private Text rawSpongeMessage;
    private MessageChannel originalChannel;
    @Nullable private MessageChannel channel;
    private Cause cause;

    @Shadow @Final public EntityPlayerMP player;

    @Shadow public abstract void setComponent(IChatComponent component);

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onConstructed(EntityPlayerMP player, String message, ChatComponentTranslation component, CallbackInfo ci) {
        // see net.minecraft.network.NetHandlerPlayServer.processChatMessage@net.minecraftforge.common.ForgeHooks.onServerChatEvent
        // this is the same way forge constructs it's messages except it
        // separates the player name and message. The above component will
        // always be a combination of the two components below when the event
        // is initialized by forge
        // - windy
        this.forgeComponent = component;
        ChatComponentTranslation sourceComponent = new ChatComponentTranslation("chat.type.text", player.getDisplayName());
        ChatComponentTranslation bodyComponent = new ChatComponentTranslation("chat.type.text", ForgeHooks.newChatWithLinks(message));

        this.formatter.getHeader().add(new DefaultHeaderApplier(SpongeTexts.toText(sourceComponent)));
        this.formatter.getBody().add(new DefaultBodyApplier(SpongeTexts.toText(bodyComponent)));

        this.rawSpongeMessage = Text.of(message);
        this.originalSpongeMessage = SpongeTexts.toText(component);
        this.originalChannel = this.channel = ((Player) player).getMessageChannel();
    }

    @Override
    public Cause getCause() {
        return this.cause;
    }

    @Override
    public void initCause(Cause cause) {
        this.cause = cause;
    }

    @Override
    public Text getOriginalMessage() {
        return this.originalSpongeMessage;
    }

    @Override
    public MessageFormatter getFormatter() {
        return this.formatter;
    }

    @Override
    public Text getRawMessage() {
        return this.rawSpongeMessage;
    }

    @Override
    public MessageChannel getOriginalChannel() {
        return this.originalChannel;
    }

    @Override
    public Optional<MessageChannel> getChannel() {
        return Optional.ofNullable(this.channel);
    }

    @Override
    public void setChannel(@Nullable MessageChannel channel) {
        this.channel = channel;
    }

    @Override
    public void syncDataToForge(Event spongeEvent) {
        super.syncDataToForge(spongeEvent);
        ServerChatEvent forgeEvent = (ServerChatEvent) spongeEvent;
        forgeEvent.setComponent(SpongeTexts.toComponent(getMessage()));
    }

    @Override
    public void syncDataToSponge(net.minecraftforge.fml.common.eventhandler.Event forgeEvent) {
        super.syncDataToSponge(forgeEvent);
        ServerChatEvent event = (ServerChatEvent) forgeEvent;
        IChatComponent component = event.getComponent();
        if (!component.equals(this.forgeComponent)) {
            setMessage(SpongeTexts.toText(event.getComponent()));
        }
    }
}
