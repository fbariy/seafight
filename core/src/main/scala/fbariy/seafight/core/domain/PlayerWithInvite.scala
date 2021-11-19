package fbariy.seafight.core.domain

case class PlayerWithInvite(override val p: Player,
                            override val opp: Player,
                            override val isFirst: Boolean,
                            invite: Invite)
    extends PlayerContext(p, opp, isFirst)
