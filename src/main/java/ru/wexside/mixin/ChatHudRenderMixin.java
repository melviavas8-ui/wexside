package ru.wexside.mixin;

import java.util.List;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.wexside.module.hud.BetterChatModule;

@Mixin(value={ChatHud.class})
public abstract class ChatHudRenderMixin {
    @Shadow
    @Final
    private List<ChatHudLine> messages;

    @Shadow
    protected abstract void refresh();

    @ModifyVariable(method={"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private Text wexside$collapseRepeatedMessage(Text message) {
        String content = message.getString();
        int repeatCount = BetterChatModule.compute(content);
        if (repeatCount <= 1) {
            return message;
        }
        String repeatedPrefix = content + " \u00d7";
        this.messages.removeIf(line -> {
            String previous = line.content().getString();
            return previous.equals(content) || previous.startsWith(repeatedPrefix);
        });
        this.refresh();
        return message.copy().append((Text)Text.literal((String)(" \u00d7" + repeatCount)).formatted(Formatting.GRAY));
    }
}

