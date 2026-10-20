CREATE TABLE IF NOT EXISTS workspace (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL CHECK(name = BTRIM(name) AND name <> ''),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS workspace_member (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL CHECK(role IN ('OWNER', 'MEMBER')),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,


    CONSTRAINT fk_workspace_member_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id) ON DELETE CASCADE,

    CONSTRAINT uq_workspace_member
    UNIQUE (workspace_id, user_id)
);

CREATE TABLE workspace_invitation(
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    invitee_user_id UUID NOT NULL,
    invited_by_user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE NULL,


    CONSTRAINT fk_workspace_invitation_workspace FOREIGN KEY (workspace_id) REFERENCES workspace(id) ON DELETE CASCADE
);

CREATE INDEX idx_workspace_member_user_id
    ON workspace_member(user_id);

CREATE INDEX idx_workspace_invitation_workspace_id
    ON workspace_invitation(workspace_id);

CREATE INDEX idx_workspace_invitation_invitee_user_id
    ON workspace_invitation(invitee_user_id);